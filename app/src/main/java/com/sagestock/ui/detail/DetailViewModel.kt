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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val ticker: String = "",
    val quote: Result<Quote> = Result.Loading,
    val indicators: Result<IndicatorSet> = Result.Loading,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: StockRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val ticker: String = checkNotNull(savedStateHandle["ticker"])

    private val _uiState = MutableStateFlow(DetailUiState(ticker = ticker))
    val uiState: StateFlow<DetailUiState> = _uiState

    init { load() }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(quote = Result.Loading, indicators = Result.Loading)
            val quoteDeferred = async { repo.getQuote(ticker) }
            val indicatorsDeferred = async { repo.getIndicators(ticker) }
            _uiState.value = _uiState.value.copy(
                quote = quoteDeferred.await(),
                indicators = indicatorsDeferred.await(),
            )
        }
    }
}
