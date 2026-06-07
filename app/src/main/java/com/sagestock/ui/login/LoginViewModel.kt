package com.sagestock.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val id: String = "",
    val password: String = "",
    val autoLogin: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun setId(value: String) = _uiState.update { it.copy(id = value, error = null) }
    fun setPassword(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun toggleAutoLogin() = _uiState.update { it.copy(autoLogin = !it.autoLogin) }

    fun login() {
        val s = _uiState.value
        if (s.id != TEST_ID || s.password != TEST_PASSWORD) {
            _uiState.update { it.copy(error = ERROR_INVALID_CREDENTIALS) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            delay(LOGIN_DELAY_MS)
            sessionManager.login(autoLogin = s.autoLogin)
            _uiState.update { it.copy(loading = false, loggedIn = true) }
        }
    }

    private companion object {
        // Phase 1 테스트 계정(스텁). 실제 인증은 Phase 7 FastAPI 연동에서 교체.
        const val TEST_ID = "admin"
        const val TEST_PASSWORD = "1234"
        const val ERROR_INVALID_CREDENTIALS = "아이디 또는 비밀번호가 올바르지 않습니다."
        const val LOGIN_DELAY_MS = 500L
    }
}
