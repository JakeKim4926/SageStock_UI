package com.sagestock.domain

enum class Market { KR, US }

data class Stock(
    val ticker: String,
    val name: String,
    val market: Market,
    val exchange: String,
)

data class Quote(
    val ticker: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
    val isDelayed: Boolean = true,
)

data class Candle(
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
)

data class IndicatorSet(
    val ticker: String,
    val rsi14: Double,
    val candles: List<Candle>,
    val rsiSeries: List<Double>,
)
