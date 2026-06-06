package com.sagestock.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.domain.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val ticker: String = "005930",
    val rsi: Result<Double> = Result.Loading,
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadRsi()
    }

    private fun loadRsi() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(rsi = Result.Loading)
            // TODO: replace with real repository call
            _uiState.value = _uiState.value.copy(rsi = Result.Success(63.4))
        }
    }
}
