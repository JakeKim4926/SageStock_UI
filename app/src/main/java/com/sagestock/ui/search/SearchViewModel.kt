package com.sagestock.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.domain.Result
import com.sagestock.domain.Stock
import com.sagestock.domain.StockRepository
import com.sagestock.domain.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: Result<List<Stock>> = Result.Success(emptyList()),
    val watchedTickers: Set<String> = emptySet(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: StockRepository,
    private val watchlistRepo: WatchlistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        queryFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach { q -> search(q) }
            .launchIn(viewModelScope)

        watchlistRepo.observeWatchlist()
            .onEach { list -> _uiState.update { it.copy(watchedTickers = list.map(Stock::ticker).toSet()) } }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(q: String) {
        _uiState.value = _uiState.value.copy(query = q)
        queryFlow.value = q
    }

    fun onWatchlistToggle(stock: Stock) {
        viewModelScope.launch { watchlistRepo.toggle(stock) }
    }

    private suspend fun search(q: String) {
        if (q.isBlank()) {
            _uiState.value = _uiState.value.copy(results = Result.Success(emptyList()))
            return
        }
        _uiState.value = _uiState.value.copy(results = Result.Loading)
        _uiState.value = _uiState.value.copy(results = repo.search(q))
    }
}
