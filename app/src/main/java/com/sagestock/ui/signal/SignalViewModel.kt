package com.sagestock.ui.signal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.domain.Result
import com.sagestock.domain.Signal
import com.sagestock.domain.SignalType
import com.sagestock.domain.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignalUiState(
    val signals: Result<List<Signal>> = Result.Loading,
    val filter: SignalType? = null,
) {
    val filtered: List<Signal>
        get() = if (signals is Result.Success) {
            if (filter == null) signals.data else signals.data.filter { it.type == filter }
        } else emptyList()
}

@HiltViewModel
class SignalViewModel @Inject constructor(
    private val repo: StockRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignalUiState())
    val uiState: StateFlow<SignalUiState> = _uiState

    init { load() }

    fun setFilter(type: SignalType?) = _uiState.update { it.copy(filter = type) }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(signals = Result.Loading) }
            _uiState.update { it.copy(signals = repo.getSignals()) }
        }
    }
}
