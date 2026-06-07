package com.sagestock.ui.paper

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagestock.domain.Market
import com.sagestock.domain.PaperRepository
import com.sagestock.domain.PaperTrade
import com.sagestock.domain.Result
import com.sagestock.domain.StockRepository
import com.sagestock.domain.TradeSide
import com.sagestock.domain.VIRTUAL_CASH_SEED
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaperInputUiState(
    val ticker: String = "",
    val name: String = "",
    val market: Market = Market.KR,
    val side: TradeSide = TradeSide.BUY,
    val currentPrice: Double = 0.0,
    val priceInput: String = "",
    val quantityInput: String = "",
    val cash: Double = VIRTUAL_CASH_SEED,
    val ownedQuantity: Int = 0,
    val saved: Boolean = false,
) {
    val price: Double get() = priceInput.toDoubleOrNull() ?: currentPrice
    val quantity: Int get() = quantityInput.toIntOrNull() ?: 0
    val orderAmount: Double get() = price * quantity
    val cashAfter: Double get() = if (side == TradeSide.BUY) cash - orderAmount else cash + orderAmount

    val error: String? get() = when {
        quantity <= 0 -> null
        side == TradeSide.BUY && orderAmount > cash -> "가상 예수금이 부족합니다."
        side == TradeSide.SELL && quantity > ownedQuantity -> "보유 수량(${ownedQuantity}주)을 초과했습니다."
        else -> null
    }
    val canSubmit: Boolean get() = quantity > 0 && error == null
}

@HiltViewModel
class PaperInputViewModel @Inject constructor(
    private val paperRepository: PaperRepository,
    private val stockRepository: StockRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val ticker: String = savedStateHandle[ARG_TICKER] ?: ""
    private val _uiState = MutableStateFlow(PaperInputUiState(ticker = ticker))
    val uiState: StateFlow<PaperInputUiState> = _uiState.asStateFlow()

    init {
        load()
        observeTrades()
    }

    private fun load() = viewModelScope.launch {
        val snapshot = (stockRepository.getMarketSnapshots() as? Result.Success)
            ?.data?.firstOrNull { it.stock.ticker == ticker }
        if (snapshot != null) {
            _uiState.value = _uiState.value.copy(
                name = snapshot.stock.name,
                market = snapshot.stock.market,
                currentPrice = snapshot.price,
                priceInput = snapshot.price.toLong().toString(),
            )
        } else {
            _uiState.value = _uiState.value.copy(name = ticker)
        }
    }

    private fun observeTrades() = viewModelScope.launch {
        paperRepository.observeTrades().collect { trades ->
            val cash = trades.fold(VIRTUAL_CASH_SEED) { acc, t ->
                if (t.side == TradeSide.BUY) acc - t.amount else acc + t.amount
            }
            val owned = trades.filter { it.ticker == ticker }
                .sumOf { if (it.side == TradeSide.BUY) it.quantity else -it.quantity }
            _uiState.value = _uiState.value.copy(cash = cash, ownedQuantity = owned)
        }
    }

    fun setSide(side: TradeSide) { _uiState.value = _uiState.value.copy(side = side) }
    fun setPrice(value: String) { _uiState.value = _uiState.value.copy(priceInput = value.filter { it.isDigit() }) }
    fun setQuantity(value: String) { _uiState.value = _uiState.value.copy(quantityInput = value.filter { it.isDigit() }) }

    /** 비율 퀵칩: 매수=예수금 기준 가능 수량, 매도=보유 기준. */
    fun applyRatio(ratio: Double) {
        val s = _uiState.value
        val price = s.price
        if (price <= 0.0) return
        val qty = if (s.side == TradeSide.BUY) (s.cash * ratio / price).toInt() else (s.ownedQuantity * ratio).toInt()
        _uiState.value = s.copy(quantityInput = qty.coerceAtLeast(0).toString())
    }

    fun submit() = viewModelScope.launch {
        val s = _uiState.value
        if (!s.canSubmit) return@launch
        paperRepository.record(
            PaperTrade(
                id = 0,
                ticker = s.ticker,
                name = s.name,
                market = s.market,
                side = s.side,
                price = s.price,
                quantity = s.quantity,
                timestamp = System.currentTimeMillis(),
            ),
        )
        _uiState.value = _uiState.value.copy(saved = true)
    }

    companion object {
        const val ARG_TICKER = "ticker"
    }
}
