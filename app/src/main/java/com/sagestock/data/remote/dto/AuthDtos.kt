package com.sagestock.data.remote.dto

import kotlinx.serialization.Serializable

/** openapi `SignupRequest`. */
@Serializable
data class SignupRequestDto(
    val email: String,
    val password: String,
    val name: String,
)

/** openapi `LoginRequest`. */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    val autoLogin: Boolean = false,
)

/** openapi `RefreshRequest`. */
@Serializable
data class RefreshRequestDto(
    val refreshToken: String,
)

/** openapi `LogoutRequest`. */
@Serializable
data class LogoutRequestDto(
    val refreshToken: String,
)

/** openapi `TokenResponse` — signup/login 발급(access+refresh). */
@Serializable
data class TokenResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Int,
)

/** openapi `AccessTokenResponse` — refresh 재발급(access만). */
@Serializable
data class AccessTokenResponseDto(
    val accessToken: String,
    val accessExpiresIn: Int,
)
