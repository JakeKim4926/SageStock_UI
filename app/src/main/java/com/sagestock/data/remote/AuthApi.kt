package com.sagestock.data.remote

import com.sagestock.data.remote.dto.AccessTokenResponseDto
import com.sagestock.data.remote.dto.LoginRequestDto
import com.sagestock.data.remote.dto.LogoutRequestDto
import com.sagestock.data.remote.dto.RefreshRequestDto
import com.sagestock.data.remote.dto.SignupRequestDto
import com.sagestock.data.remote.dto.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

// Auth endpoints (openapi /v1/auth/*). Called via @AuthHttpClient (no interceptor/authenticator)
// so token refresh does not recurse.
interface AuthApi {

    @POST("v1/auth/signup")
    suspend fun signup(@Body body: SignupRequestDto): TokenResponseDto

    @POST("v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenResponseDto

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): AccessTokenResponseDto

    @POST("v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequestDto)
}
