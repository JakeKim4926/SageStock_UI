package com.sagestock.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.domain.Market
import com.sagestock.domain.MarketStatus
import com.sagestock.domain.Prediction
import com.sagestock.domain.PredictionStatus
import com.sagestock.domain.Result
import com.sagestock.domain.Signal
import com.sagestock.domain.Stock
import com.sagestock.domain.StockRepository
import com.sagestock.domain.StockSnapshot
import com.sagestock.domain.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** 상위 종목 세그먼트(상승률/거래량/AI 후보). */
enum class TopFilter { RISERS, VOLUME, AI }

data class HomeUiState(
    val date: String = "",
    val krStatus: MarketStatus = MarketStatus.CLOSED,
    val usStatus: MarketStatus = MarketStatus.CLOSED,
    val loading: Boolean = true,
    val watchFilter: Market? = null,
    val topFilter: TopFilter = TopFilter.RISERS,
    val snapshots: List<StockSnapshot> = emptyList(),
    val watchedSnapshots: List<StockSnapshot> = emptyList(),
    val signals: List<Signal> = emptyList(),
    val predictions: List<Prediction> = emptyList(),
) {
    val watchlist: List<StockSnapshot>
        get() = watchedSnapshots.filter { watchFilter == null || it.stock.market == watchFilter }

    val topMovers: List<StockSnapshot>
        get() = when (topFilter) {
            TopFilter.RISERS -> snapshots.sortedByDescending { it.changePercent }
            TopFilter.VOLUME -> snapshots.sortedByDescending { it.volume }
            TopFilter.AI -> {
                val aiTickers = aiCandidates.map { it.stock.ticker }.toSet()
                snapshots.filter { it.stock.ticker in aiTickers }
            }
        }.take(MAX_TOP_MOVERS)

    val aiCandidates: List<Prediction>
        get() = predictions.filter { it.status == PredictionStatus.READY }

    val todaySignals: List<Signal> get() = signals.take(MAX_HOME_SIGNALS)

    private companion object {
        const val MAX_TOP_MOVERS = 5
        const val MAX_HOME_SIGNALS = 5
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val watchlistRepository: WatchlistRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(date = todayLabel(), krStatus = marketStatus(Market.KR), usStatus = marketStatus(Market.US)))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var watchedStocks: List<Stock> = emptyList()
    private var rebuildJob: Job? = null

    init {
        watchlistRepository.observeWatchlist()
            .onEach { stocks -> watchedStocks = stocks; rebuildWatchlist() }
            .launchIn(viewModelScope)
        load()
    }

    fun setWatchFilter(market: Market?) = _uiState.update { it.copy(watchFilter = market) }
    fun setTopFilter(filter: TopFilter) = _uiState.update { it.copy(topFilter = filter) }

    fun removeWatched(ticker: String) = viewModelScope.launch { watchlistRepository.remove(ticker) }
    fun clearWatchlist() = viewModelScope.launch { watchlistRepository.clearAll() }

    private fun load() = viewModelScope.launch {
        val snapshots = (stockRepository.getMarketSnapshots() as? Result.Success)?.data ?: emptyList()
        val signals = (stockRepository.getSignals() as? Result.Success)?.data ?: emptyList()
        val predictions = (stockRepository.getPredictions() as? Result.Success)?.data ?: emptyList()
        _uiState.update {
            it.copy(loading = false, snapshots = snapshots, signals = signals, predictions = predictions)
        }
        rebuildWatchlist()
    }

    /**
     * 관심종목 시세 채우기. 시장 스냅샷에 있으면 재사용(스파크라인 포함), 없으면 [StockRepository.getQuote]로
     * 보강한다. 스냅샷에 없다고 홈에서 누락되던 버그를 막는다. watchlist/snapshots 변경 시마다 재계산.
     */
    private fun rebuildWatchlist() {
        rebuildJob?.cancel()
        rebuildJob = viewModelScope.launch {
            val bySnapshot = _uiState.value.snapshots.associateBy { it.stock.ticker }
            val stocks = watchedStocks
            val result = coroutineScope {
                stocks.map { stock -> async { bySnapshot[stock.ticker] ?: fetchSnapshot(stock) } }.awaitAll()
            }
            _uiState.update { it.copy(watchedSnapshots = result) }
        }
    }

    private suspend fun fetchSnapshot(stock: Stock): StockSnapshot {
        val quote = (stockRepository.getQuote(stock.ticker) as? Result.Success)?.data
        return StockSnapshot(
            stock = stock,
            price = quote?.price ?: 0.0,
            change = quote?.change ?: 0.0,
            changePercent = quote?.changePercent ?: 0.0,
            volume = quote?.volume ?: 0L,
        )
    }

    private companion object {
        fun todayLabel(): String =
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN))

        /** 시각 기반 장 상태(뷰어 스텁 — 실데이터는 Phase 7 백엔드). 주말은 휴장. */
        fun marketStatus(market: Market): MarketStatus {
            val zone = if (market == Market.KR) ZoneId.of("Asia/Seoul") else ZoneId.of("America/New_York")
            val now = ZonedDateTime.now(zone)
            if (now.dayOfWeek.value >= 6) return MarketStatus.CLOSED
            val t = now.toLocalTime()
            return if (market == Market.KR) {
                if (t.isInRange(9, 0, 15, 30)) MarketStatus.OPEN else MarketStatus.CLOSED
            } else {
                when {
                    t.isInRange(4, 0, 9, 30) -> MarketStatus.PRE_MARKET
                    t.isInRange(9, 30, 16, 0) -> MarketStatus.OPEN
                    t.isInRange(16, 0, 20, 0) -> MarketStatus.AFTER_MARKET
                    else -> MarketStatus.CLOSED
                }
            }
        }

        private fun LocalTime.isInRange(fromH: Int, fromM: Int, toH: Int, toM: Int): Boolean {
            val from = LocalTime.of(fromH, fromM)
            val to = LocalTime.of(toH, toM)
            return !isBefore(from) && isBefore(to)
        }
    }
}
