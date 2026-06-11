package com.sagestock.domain

/**
 * 인증/세션 계약(api-spec §1). 성공 시 토큰을 세션에 저장하는 책임은 구현체에 있다.
 * UI는 성공/실패만 알면 되므로 `Result<Unit>`을 반환한다.
 */
interface AuthRepository {
    suspend fun signup(email: String, password: String, name: String): Result<Unit>
    suspend fun login(email: String, password: String, autoLogin: Boolean): Result<Unit>
    suspend fun logout(): Result<Unit>
}
