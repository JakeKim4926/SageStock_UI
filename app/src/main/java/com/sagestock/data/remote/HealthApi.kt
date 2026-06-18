package com.sagestock.data.remote

import retrofit2.http.GET

/**
 * Render 워밍업·heartbeat용 헬스체크. 루트 `/health`(순수 200, DB 미접근).
 * 무인증 plain 클라이언트(@AuthHttpClient)로 호출 — 토큰 인터셉터/Authenticator를 타지 않는다.
 */
interface HealthApi {
    @GET("health")
    suspend fun ping()
}
