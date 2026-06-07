package com.sagestock.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.domain.IndicatorSet
import com.sagestock.domain.Quote
import com.sagestock.domain.Result
import com.sagestock.domain.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IndicatorConfig(
    val showRsi: Boolean = true,
    val showEma: Boolean = false,
    val showBollinger: Boolean = false,
    val showStochastic: Boolean = false,
    val showDisparity: Boolean = false,
)

data class DetailUiState(
    val ticker: String = "",
    val quote: Result<Quote> = Result.Loading,
    val indicators: Result<IndicatorSet> = Result.Loading,
    val config: IndicatorConfig = IndicatorConfig(),
    val highlightIndex: Int = -1,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: StockRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val ticker: String = checkNotNull(savedStateHandle[ARG_TICKER])
    private val initialHighlight: Int = savedStateHandle[ARG_INDEX] ?: -1

    private val _uiState = MutableStateFlow(DetailUiState(ticker = ticker, highlightIndex = initialHighlight))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    fun toggleRsi() = _uiState.update { it.copy(config = it.config.copy(showRsi = !it.config.showRsi)) }
    fun toggleEma() = _uiState.update { it.copy(config = it.config.copy(showEma = !it.config.showEma)) }
    fun toggleBollinger() = _uiState.update { it.copy(config = it.config.copy(showBollinger = !it.config.showBollinger)) }
    fun toggleStochastic() = _uiState.update { it.copy(config = it.config.copy(showStochastic = !it.config.showStochastic)) }
    fun toggleDisparity() = _uiState.update { it.copy(config = it.config.copy(showDisparity = !it.config.showDisparity)) }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(quote = Result.Loading, indicators = Result.Loading) }
            val quoteDeferred = async { repo.getQuote(ticker) }
            val indicatorsDeferred = async { repo.getIndicators(ticker) }
            _uiState.update { it.copy(
                quote = quoteDeferred.await(),
                indicators = indicatorsDeferred.await(),
            )}
        }
    }

    companion object {
        const val ARG_TICKER = "ticker"
        const val ARG_INDEX = "index"
    }
}
