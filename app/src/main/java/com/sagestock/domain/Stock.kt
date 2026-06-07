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
    val market: Market = Market.KR,
)

data class Candle(
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
)

enum class CrossType { GOLDEN, DEAD }
data class CrossMarker(val index: Int, val type: CrossType)

data class IndicatorSet(
    val ticker: String,
    val rsi14: Double,
    val candles: List<Candle>,
    val rsiSeries: List<Double>,
    val ema5: List<Double> = emptyList(),
    val ema20: List<Double> = emptyList(),
    val ema60: List<Double> = emptyList(),
    val ema120: List<Double> = emptyList(),
    val bollingerUpper: List<Double> = emptyList(),
    val bollingerMid: List<Double> = emptyList(),
    val bollingerLower: List<Double> = emptyList(),
    val disparitySeries: List<Double> = emptyList(),
    val stochasticK: List<Double> = emptyList(),
    val stochasticD: List<Double> = emptyList(),
    val crossMarkers: List<CrossMarker> = emptyList(),
    val divergenceMarkers: List<Int> = emptyList(),
)

enum class SignalType { GOLDEN_CROSS, DEAD_CROSS, RSI_OVERSOLD, RSI_OVERBOUGHT, BOLLINGER_BREAKOUT }
enum class RiskLevel { LOW, MEDIUM, HIGH }

data class Signal(
    val id: String,
    val ticker: String,
    val stockName: String,
    val type: SignalType,
    val date: String,
    val description: String,
    val riskLevel: RiskLevel,
    val candleIndex: Int = -1,
)
