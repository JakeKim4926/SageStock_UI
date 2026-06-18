package com.sagestock.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.domain.IndicatorSet
import com.sagestock.domain.Quote
import com.sagestock.domain.Result
import com.sagestock.domain.Signal
import com.sagestock.domain.Stock
import com.sagestock.domain.StockRepository
import com.sagestock.domain.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 상세 내부 탭(와이어프레임 04). */
enum class DetailTab(val label: String) { CHART("차트"), INDICATORS("지표"), SIGNALS("시그널"), PAPER("가상매매") }

/**
 * 차트 조회 범위(range) — 얼마나 거슬러 볼지. apiValue = 백엔드 `range` 쿼리값.
 * 캔들 간격(일/주/월)은 [CandleUnit]이 담당한다. 분봉이 없어 1D·5D는 두지 않는다.
 */
enum class ChartPeriod(val label: String, val apiValue: String) {
    M1("1개월", "1m"), M3("3개월", "3m"), M6("6개월", "6m"), Y1("1년", "1y"), ALL("전체", "max")
}

/** 봉 간격(interval) — apiValue = 백엔드 `interval` 쿼리값. */
enum class CandleUnit(val label: String, val apiValue: String) { DAY("일봉", "1d"), WEEK("주봉", "1w"), MONTH("월봉", "1mo") }

data class IndicatorConfig(
    val showRsi: Boolean = true,
    val showEma: Boolean = false,
    val showBollinger: Boolean = false,
    val showStochastic: Boolean = false,
    val showDisparity: Boolean = false,
)

data class DetailUiState(
    val ticker: String = "",
    val stock: Stock? = null,
    val quote: Result<Quote> = Result.Loading,
    val indicators: Result<IndicatorSet> = Result.Loading,
    val config: IndicatorConfig = IndicatorConfig(),
    val highlightIndex: Int = -1,
    val selectedTab: DetailTab = DetailTab.CHART,
    val period: ChartPeriod = ChartPeriod.M3,
    val candleUnit: CandleUnit = CandleUnit.DAY,
    val signals: List<Signal> = emptyList(),
    val isWatched: Boolean = false,
    val showSettings: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repo: StockRepository,
    private val watchlistRepo: WatchlistRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val ticker: String = checkNotNull(savedStateHandle[ARG_TICKER])
    private val initialHighlight: Int = savedStateHandle[ARG_INDEX] ?: -1

    private val _uiState = MutableStateFlow(DetailUiState(ticker = ticker, highlightIndex = initialHighlight))
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    // (간격, 범위)별 지표 캐시 — 한 번 받은 조합은 재조회 없이 즉시 표시. 조합이 3×5=15개로 유한하고
    // ViewModel(상세 화면) 수명에만 살아 자연 bounded.
    private val indicatorCache = mutableMapOf<Pair<CandleUnit, ChartPeriod>, IndicatorSet>()

    init {
        load()
        observeWatchlist()
    }

    fun retry() = load()

    fun selectTab(tab: DetailTab) = _uiState.update { it.copy(selectedTab = tab) }

    fun setPeriod(period: ChartPeriod) {
        if (period == _uiState.value.period) return
        _uiState.update { it.copy(period = period) }
        refreshIndicators()
    }

    fun setCandleUnit(unit: CandleUnit) {
        if (unit == _uiState.value.candleUnit) return
        _uiState.update { it.copy(candleUnit = unit) }
        refreshIndicators()
    }
    fun openSettings() = _uiState.update { it.copy(showSettings = true) }
    fun closeSettings() = _uiState.update { it.copy(showSettings = false) }

    fun toggleWatch() {
        val stock = _uiState.value.stock ?: return
        viewModelScope.launch { watchlistRepo.toggle(stock) }
    }

    fun toggleRsi() = _uiState.update { it.copy(config = it.config.copy(showRsi = !it.config.showRsi)) }
    fun toggleEma() = _uiState.update { it.copy(config = it.config.copy(showEma = !it.config.showEma)) }
    fun toggleBollinger() = _uiState.update { it.copy(config = it.config.copy(showBollinger = !it.config.showBollinger)) }
    fun toggleStochastic() = _uiState.update { it.copy(config = it.config.copy(showStochastic = !it.config.showStochastic)) }
    fun toggleDisparity() = _uiState.update { it.copy(config = it.config.copy(showDisparity = !it.config.showDisparity)) }

    private fun observeWatchlist() {
        watchlistRepo.observeWatchlist()
            .onEach { list -> _uiState.update { it.copy(isWatched = list.any { s -> s.ticker == ticker }) } }
            .launchIn(viewModelScope)
    }

    private fun load() {
        val unit = _uiState.value.candleUnit
        val period = _uiState.value.period
        viewModelScope.launch {
            _uiState.update { it.copy(quote = Result.Loading, indicators = Result.Loading) }
            val quoteDeferred = async { repo.getQuote(ticker) }
            val indicatorsDeferred = async { repo.getIndicators(ticker, unit.apiValue, period.apiValue) }
            val stockDeferred = async { (repo.search(ticker) as? Result.Success)?.data?.firstOrNull { it.ticker == ticker } }
            val signalsDeferred = async { (repo.getSignals() as? Result.Success)?.data?.filter { it.ticker == ticker } ?: emptyList() }
            val indicators = indicatorsDeferred.await()
            if (indicators is Result.Success) indicatorCache[unit to period] = indicators.data
            _uiState.update {
                it.copy(
                    quote = quoteDeferred.await(),
                    indicators = indicators,
                    stock = stockDeferred.await(),
                    signals = signalsDeferred.await(),
                )
            }
        }
    }

    /** 봉/기간 변경 시 지표만 재조회. 캐시에 있으면 즉시 표시, 없으면 조회 후 캐시. */
    private fun refreshIndicators() {
        val unit = _uiState.value.candleUnit
        val period = _uiState.value.period
        indicatorCache[unit to period]?.let { cached ->
            _uiState.update { it.copy(indicators = Result.Success(cached)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(indicators = Result.Loading) }
            val result = repo.getIndicators(ticker, unit.apiValue, period.apiValue)
            if (result is Result.Success) indicatorCache[unit to period] = result.data
            _uiState.update { it.copy(indicators = result) }
        }
    }

    companion object {
        const val ARG_TICKER = "ticker"
        const val ARG_INDEX = "index"
    }
}
