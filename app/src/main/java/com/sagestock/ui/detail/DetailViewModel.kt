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
 * 차트 조회 범위(range) — 얼마나 거슬러 볼지. days = 표시할 일봉 수(전체는 제한 없음).
 * 캔들 간격(일/주/월)은 [CandleUnit]이 담당한다. 분봉이 없어 1D·5D는 두지 않는다.
 */
enum class ChartPeriod(val label: String, val days: Int) {
    M1("1개월", 22), M3("3개월", 66), M6("6개월", 132), Y1("1년", 264), ALL("전체", Int.MAX_VALUE)
}

/** 봉 간격(interval) — 캔들 하나가 며칠치냐. */
enum class CandleUnit(val label: String, val groupSize: Int) { DAY("일봉", 1), WEEK("주봉", 5), MONTH("월봉", 22) }

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

    init {
        load()
        observeWatchlist()
    }

    fun retry() = load()

    fun selectTab(tab: DetailTab) = _uiState.update { it.copy(selectedTab = tab) }
    fun setPeriod(period: ChartPeriod) = _uiState.update { it.copy(period = period) }
    fun setCandleUnit(unit: CandleUnit) = _uiState.update { it.copy(candleUnit = unit) }
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
        viewModelScope.launch {
            _uiState.update { it.copy(quote = Result.Loading, indicators = Result.Loading) }
            val quoteDeferred = async { repo.getQuote(ticker) }
            val indicatorsDeferred = async { repo.getIndicators(ticker) }
            val stockDeferred = async { (repo.search(ticker) as? Result.Success)?.data?.firstOrNull { it.ticker == ticker } }
            val signalsDeferred = async { (repo.getSignals() as? Result.Success)?.data?.filter { it.ticker == ticker } ?: emptyList() }
            _uiState.update {
                it.copy(
                    quote = quoteDeferred.await(),
                    indicators = indicatorsDeferred.await(),
                    stock = stockDeferred.await(),
                    signals = signalsDeferred.await(),
                )
            }
        }
    }

    companion object {
        const val ARG_TICKER = "ticker"
        const val ARG_INDEX = "index"
    }
}
