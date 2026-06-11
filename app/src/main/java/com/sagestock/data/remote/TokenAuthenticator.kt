package com.sagestock.data.remote

import com.sagestock.data.SessionManager
import com.sagestock.data.remote.dto.RefreshRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * 401 응답 시 refresh 토큰으로 access를 1회 재발급하고 원요청을 재시도(api-spec §0).
 * refresh도 실패하면 세션을 비우고 포기(`null` → 호출부에서 401 그대로 노출).
 *
 * [AuthApi]는 인터셉터/Authenticator가 없는 [AuthHttpClient]로 만들어 순환 호출을 막는다.
 */
class TokenAuthenticator @Inject constructor(
    private val session: SessionManager,
    private val authApi: AuthApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_ATTEMPTS) return null
        val refresh = session.refreshToken() ?: return null

        val newToken = runBlocking {
            runCatching { authApi.refresh(RefreshRequestDto(refresh)) }.getOrNull()
        } ?: run {
            session.logout()
            return null
        }

        session.updateAccessToken(newToken.accessToken, newToken.accessExpiresIn)
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newToken.accessToken}")
            .build()
    }

    /** 이번 요청에 대해 누적된 응답 수(재시도 횟수 제한용). */
    private fun responseCount(response: Response): Int {
        var prior = response.priorResponse
        var count = 1
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
    }
}
