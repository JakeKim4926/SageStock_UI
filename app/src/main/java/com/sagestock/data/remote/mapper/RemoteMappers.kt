package com.sagestock.data.remote.mapper

import com.sagestock.data.remote.dto.CandleDto
import com.sagestock.data.remote.dto.CrossMarkerDto
import com.sagestock.data.remote.dto.IndicatorSetDto
import com.sagestock.data.remote.dto.QuoteDto
import com.sagestock.data.remote.dto.SignalDto
import com.sagestock.data.remote.dto.SignalScoreDto
import com.sagestock.data.remote.dto.StockDto
import com.sagestock.data.remote.dto.StockSnapshotDto
import com.sagestock.domain.Candle
import com.sagestock.domain.CrossMarker
import com.sagestock.domain.CrossType
import com.sagestock.domain.IndicatorSet
import com.sagestock.domain.Market
import com.sagestock.domain.Quote
import com.sagestock.domain.RiskLevel
import com.sagestock.domain.Signal
import com.sagestock.domain.SignalScore
import com.sagestock.domain.SignalType
import com.sagestock.domain.Stock
import com.sagestock.domain.StockSnapshot

// 와이어 enum 문자열 → 도메인 enum. 와이어 값 = Kotlin enum 이름(SCREAMING_SNAKE)으로 동결(api-spec §0).

fun StockDto.toDomain() = Stock(
    ticker = ticker,
    name = name,
    market = Market.valueOf(market),
    exchange = exchange,
)

fun QuoteDto.toDomain() = Quote(
    ticker = ticker,
    price = price,
    change = change,
    changePercent = changePercent,
    open = open,
    high = high,
    low = low,
    volume = volume,
    isDelayed = isDelayed,
    market = Market.valueOf(market),
)

fun StockSnapshotDto.toDomain() = StockSnapshot(
    stock = stock.toDomain(),
    price = price,
    change = change,
    changePercent = changePercent,
    volume = volume,
    sparkline = sparkline,
)

fun CandleDto.toDomain() = Candle(
    date = date,
    open = open,
    high = high,
    low = low,
    close = close,
    volume = volume,
)

fun CrossMarkerDto.toDomain() = CrossMarker(
    index = index,
    type = CrossType.valueOf(type),
)

fun IndicatorSetDto.toDomain() = IndicatorSet(
    ticker = ticker,
    rsi14 = rsi14,
    candles = candles.map { it.toDomain() },
    rsiSeries = rsiSeries,
    ema5 = ema5,
    ema20 = ema20,
    ema60 = ema60,
    ema120 = ema120,
    bollingerUpper = bollingerUpper,
    bollingerMid = bollingerMid,
    bollingerLower = bollingerLower,
    disparitySeries = disparitySeries,
    stochasticK = stochasticK,
    stochasticD = stochasticD,
    crossMarkers = crossMarkers.map { it.toDomain() },
    divergenceMarkers = divergenceMarkers,
)

fun SignalDto.toDomain() = Signal(
    id = id,
    ticker = ticker,
    stockName = stockName,
    type = SignalType.valueOf(type),
    date = date,
    description = description,
    riskLevel = RiskLevel.valueOf(riskLevel),
    candleIndex = candleIndex,
)

fun SignalScoreDto.toDomain() = SignalScore(
    stock = stock.toDomain(),
    score = score,
    buySignals = buySignals.map { SignalType.valueOf(it) },
    sellSignals = sellSignals.map { SignalType.valueOf(it) },
)
