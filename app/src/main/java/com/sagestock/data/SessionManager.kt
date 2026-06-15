package com.sagestock.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 단일 사용자 세션. 자동 로그인 플래그(Phase 1)와 JWT 토큰(Phase 7)을 함께 보관한다.
 * 토큰 발급/회전은 [com.sagestock.data.RetrofitAuthRepository]와
 * [com.sagestock.data.remote.TokenAuthenticator]가 갱신한다.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    /** 자동 로그인으로 살아있는 세션이 있으면 true → 스플래시에서 홈 직행. */
    fun isAutoLoggedIn(): Boolean = prefs.getBoolean(KEY_ACTIVE, false)

    fun login(autoLogin: Boolean) {
        prefs.edit { putBoolean(KEY_ACTIVE, autoLogin) }
    }

    /** 로그아웃 — 자동 로그인 플래그와 토큰을 모두 비운다. */
    fun logout() {
        prefs.edit { clear() }
    }

    // --- 토큰 (Phase 7) ---

    fun accessToken(): String? = prefs.getString(KEY_ACCESS, null)

    fun refreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    /** signup/login 성공 시 access+refresh 저장. */
    fun saveTokens(accessToken: String, refreshToken: String, accessExpiresInSec: Int) {
        prefs.edit {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
            putLong(KEY_ACCESS_EXPIRES_AT, expiresAt(accessExpiresInSec))
        }
    }

    /** refresh 재발급 시 access만 교체(refresh 토큰은 유지). */
    fun updateAccessToken(accessToken: String, accessExpiresInSec: Int) {
        prefs.edit {
            putString(KEY_ACCESS, accessToken)
            putLong(KEY_ACCESS_EXPIRES_AT, expiresAt(accessExpiresInSec))
        }
    }

    private fun expiresAt(expiresInSec: Int): Long =
        System.currentTimeMillis() + expiresInSec * MILLIS_PER_SEC

    private companion object {
        const val KEY_ACTIVE = "active"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
        const val MILLIS_PER_SEC = 1000L
    }
}
