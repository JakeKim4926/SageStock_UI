package com.sagestock.ui.splash

import androidx.lifecycle.ViewModel
import com.sagestock.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    sessionManager: SessionManager,
) : ViewModel() {
    /** 자동 로그인 세션 유무 → 스플래시에서 홈/로그인 분기. */
    val autoLoggedIn: Boolean = sessionManager.isAutoLoggedIn()
}
