package com.sagestock.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.domain.AuthRepository
import com.sagestock.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun setId(value: String) = _uiState.update { it.copy(id = value, error = null) }
    fun setPassword(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun toggleAutoLogin() = _uiState.update { it.copy(autoLogin = !it.autoLogin) }

    fun login() {
        val s = _uiState.value
        if (s.id.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(error = ERROR_INVALID_CREDENTIALS) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            // 입력란은 "아이디"지만 서버(api-spec §1)는 email을 받으므로 그대로 전달한다.
            when (val result = authRepository.login(s.id, s.password, s.autoLogin)) {
                is Result.Success -> _uiState.update { it.copy(loading = false, loggedIn = true) }
                is Result.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    private companion object {
        const val ERROR_INVALID_CREDENTIALS = "아이디 또는 비밀번호가 올바르지 않습니다."
    }
}
