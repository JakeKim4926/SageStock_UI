package com.sagestock.data.remote

import com.sagestock.data.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** 보호 엔드포인트 요청에 `Authorization: Bearer <accessToken>`를 부착(api-spec §0). */
class AuthInterceptor @Inject constructor(
    private val session: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.accessToken()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
