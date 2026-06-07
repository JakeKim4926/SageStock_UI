package com.sagestock.ui.prediction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.domain.Prediction
import com.sagestock.domain.Result
import com.sagestock.domain.Stock
import com.sagestock.domain.StockRepository
import com.sagestock.domain.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PredictionUiState(
    val predictions: Result<List<Prediction>> = Result.Loading,
    val watchedTickers: Set<String> = emptySet(),
)

@HiltViewModel
class PredictionViewModel @Inject constructor(
    private val repo: StockRepository,
    private val watchlistRepo: WatchlistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PredictionUiState())
    val uiState: StateFlow<PredictionUiState> = _uiState.asStateFlow()

    init {
        load()
        watchlistRepo.observeWatchlist()
            .onEach { list -> _uiState.update { it.copy(watchedTickers = list.map(Stock::ticker).toSet()) } }
            .launchIn(viewModelScope)
    }

    fun retry() = load()

    fun onWatchlistToggle(stock: Stock) {
        viewModelScope.launch { watchlistRepo.toggle(stock) }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(predictions = Result.Loading) }
            _uiState.update { it.copy(predictions = repo.getPredictions()) }
        }
    }
}
