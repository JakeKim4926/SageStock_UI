package com.sagestock.ui.paper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.domain.Holding
import com.sagestock.domain.PaperRepository
import com.sagestock.domain.PaperTrade
import com.sagestock.domain.Result
import com.sagestock.domain.Stock
import com.sagestock.domain.StockRepository
import com.sagestock.domain.TradeSide
import com.sagestock.domain.VIRTUAL_CASH_SEED
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PortfolioTab { HOLDINGS, HISTORY }

data class PaperUiState(
    val loading: Boolean = true,
    val tab: PortfolioTab = PortfolioTab.HOLDINGS,
    val holdings: List<Holding> = emptyList(),
    val history: List<PaperTrade> = emptyList(),
    val cash: Double = VIRTUAL_CASH_SEED,
    val totalValue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val totalProfitPercent: Double = 0.0,
) {
    val isEmpty: Boolean get() = holdings.isEmpty() && history.isEmpty()
}

@HiltViewModel
class PaperViewModel @Inject constructor(
    private val paperRepository: PaperRepository,
    private val stockRepository: StockRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaperUiState())
    val uiState: StateFlow<PaperUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val priceByTicker = (stockRepository.getMarketSnapshots() as? Result.Success)
                ?.data?.associate { it.stock.ticker to it.price } ?: emptyMap()
            paperRepository.observeTrades().collect { trades ->
                _uiState.value = build(trades, priceByTicker)
            }
        }
    }

    fun setTab(tab: PortfolioTab) = run { _uiState.value = _uiState.value.copy(tab = tab) }

    private fun build(trades: List<PaperTrade>, priceByTicker: Map<String, Double>): PaperUiState {
        val holdings = aggregate(trades, priceByTicker)
        val cash = trades.fold(VIRTUAL_CASH_SEED) { acc, t ->
            if (t.side == TradeSide.BUY) acc - t.amount else acc + t.amount
        }
        val totalValue = holdings.sumOf { it.value }
        val totalInvested = holdings.sumOf { it.invested }
        val totalProfit = totalValue - totalInvested
        val totalProfitPercent = if (totalInvested == 0.0) 0.0 else totalProfit / totalInvested * 100
        return _uiState.value.copy(
            loading = false,
            holdings = holdings,
            history = trades,
            cash = cash,
            totalValue = totalValue,
            totalProfit = totalProfit,
            totalProfitPercent = totalProfitPercent,
        )
    }

    /** 종목별 순보유로 집계(매수 가중 평단). 순수량 0 이하 종목은 제외. */
    private fun aggregate(trades: List<PaperTrade>, priceByTicker: Map<String, Double>): List<Holding> {
        return trades.groupBy { it.ticker }.mapNotNull { (_, list) ->
            var qty = 0
            var buyQty = 0
            var buyCost = 0.0
            list.sortedBy { it.timestamp }.forEach { t ->
                if (t.side == TradeSide.BUY) {
                    qty += t.quantity; buyQty += t.quantity; buyCost += t.amount
                } else {
                    qty -= t.quantity
                }
            }
            if (qty <= 0) return@mapNotNull null
            val sample = list.first()
            val avgPrice = if (buyQty == 0) 0.0 else buyCost / buyQty
            Holding(
                stock = Stock(sample.ticker, sample.name, sample.market, sample.market.name),
                quantity = qty,
                avgPrice = avgPrice,
                currentPrice = priceByTicker[sample.ticker] ?: avgPrice,
            )
        }
    }
}
