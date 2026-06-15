package com.sagestock.data

import com.sagestock.data.remote.AuthApi
import com.sagestock.data.remote.dto.LoginRequestDto
import com.sagestock.data.remote.dto.LogoutRequestDto
import com.sagestock.data.remote.dto.SignupRequestDto
import com.sagestock.data.remote.dto.TokenResponseDto
import com.sagestock.data.remote.safeApiCall
import com.sagestock.di.IoDispatcher
import com.sagestock.domain.AuthRepository
import com.sagestock.domain.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Phase 7 실서버 인증 구현(api-spec §1). 성공 시 [SessionManager]에 토큰을 저장한다. */
@Singleton
class RetrofitAuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val session: SessionManager,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun signup(email: String, password: String, name: String): Result<Unit> =
        safeApiCall(io, json) {
            authApi.signup(SignupRequestDto(email, password, name)).persist(autoLogin = true)
        }

    override suspend fun login(email: String, password: String, autoLogin: Boolean): Result<Unit> =
        safeApiCall(io, json) {
            authApi.login(LoginRequestDto(email, password, autoLogin)).persist(autoLogin)
        }

    override suspend fun logout(): Result<Unit> = withContext(io) {
        val refresh = session.refreshToken()
        val result = try {
            if (refresh != null) authApi.logout(LogoutRequestDto(refresh))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "로그아웃에 실패했습니다.")
        } finally {
            session.logout() // 네트워크 결과와 무관하게 로컬 세션은 항상 정리
        }
        result
    }

    private fun TokenResponseDto.persist(autoLogin: Boolean) {
        session.saveTokens(accessToken, refreshToken, accessExpiresIn)
        session.login(autoLogin)
    }
}
