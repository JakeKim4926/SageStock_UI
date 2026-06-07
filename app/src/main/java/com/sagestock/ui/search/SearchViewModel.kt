package com.sagestock.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.domain.Market
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
    val marketFilter: Market? = null,
    val recentSearches: List<String> = emptyList(),
) {
    /** 시장 필터를 적용한 결과(원본 results는 보존). */
    val displayResults: Result<List<Stock>>
        get() = when (results) {
            is Result.Success -> Result.Success(
                results.data.filter { marketFilter == null || it.market == marketFilter },
            )
            else -> results
        }
}

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

    fun setMarketFilter(market: Market?) = _uiState.update { it.copy(marketFilter = market) }

    /** 종목 진입 시 최근 검색에 추가(최신 우선, 중복 제거, 최대 [MAX_RECENTS]). */
    fun addRecent(term: String) = _uiState.update {
        it.copy(recentSearches = (listOf(term) + it.recentSearches).distinct().take(MAX_RECENTS))
    }

    fun removeRecent(term: String) = _uiState.update { it.copy(recentSearches = it.recentSearches - term) }

    fun clearRecents() = _uiState.update { it.copy(recentSearches = emptyList()) }

    private suspend fun search(q: String) {
        if (q.isBlank()) {
            _uiState.value = _uiState.value.copy(results = Result.Success(emptyList()))
            return
        }
        _uiState.value = _uiState.value.copy(results = Result.Loading)
        _uiState.value = _uiState.value.copy(results = repo.search(q))
    }

    private companion object {
        const val MAX_RECENTS = 8
    }
}
