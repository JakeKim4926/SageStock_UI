package com.sagestock.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 단일 사용자 세션 스텁(Phase 1). 자동 로그인 체크 시에만 세션을 영속화해
 * 재실행 시 스플래시에서 홈으로 직행한다. Phase 7에서 실제 토큰으로 교체.
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

    fun logout() {
        prefs.edit { putBoolean(KEY_ACTIVE, false) }
    }

    private companion object {
        const val KEY_ACTIVE = "active"
    }
}
