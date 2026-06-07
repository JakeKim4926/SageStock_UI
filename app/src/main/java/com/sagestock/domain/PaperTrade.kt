package com.sagestock.domain

enum class TradeSide { BUY, SELL }

/** 가상매매 1건의 체결 기록(실제 주문 아님). */
data class PaperTrade(
    val id: Long,
    val ticker: String,
    val name: String,
    val market: Market,
    val side: TradeSide,
    val price: Double,
    val quantity: Int,
    val timestamp: Long,
) {
    val amount: Double get() = price * quantity
}

/** 매매 기록을 종목별로 집계한 보유 현황. */
data class Holding(
    val stock: Stock,
    val quantity: Int,
    val avgPrice: Double,
    val currentPrice: Double,
) {
    val invested: Double get() = avgPrice * quantity
    val value: Double get() = currentPrice * quantity
    val profit: Double get() = value - invested
    val profitPercent: Double get() = if (invested == 0.0) 0.0 else profit / invested * 100
}

/** 가상매매 시작 예수금(Mock). */
const val VIRTUAL_CASH_SEED = 10_000_000.0
