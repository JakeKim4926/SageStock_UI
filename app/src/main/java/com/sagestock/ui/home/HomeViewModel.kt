package com.sagestock.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.domain.Stock
import com.sagestock.domain.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val watchlist: List<Stock> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    watchlistRepo: WatchlistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        watchlistRepo.observeWatchlist()
            .onEach { list -> _uiState.update { it.copy(watchlist = list) } }
            .launchIn(viewModelScope)
    }
}
