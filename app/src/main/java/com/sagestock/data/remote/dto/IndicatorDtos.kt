package com.sagestock.data.remote.dto

import kotlinx.serialization.Serializable

/** openapi `CandleResponse` — 일봉 OHLCV. */
@Serializable
data class CandleDto(
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
)

/** openapi `CrossMarkerResponse` — 차트 위 교차 마커(candles 인덱스 기준). */
@Serializable
data class CrossMarkerDto(
    val index: Int,
    val type: String,
)

/**
 * openapi `IndicatorSetResponse` — 차트 오버레이용 지표 시리즈.
 * 모든 시리즈는 candles 인덱스와 정렬된다(차트 오버레이 기준).
 */
@Serializable
data class IndicatorSetDto(
    val ticker: String,
    val rsi14: Double,
    val candles: List<CandleDto> = emptyList(),
    val rsiSeries: List<Double> = emptyList(),
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
    val crossMarkers: List<CrossMarkerDto> = emptyList(),
    val divergenceMarkers: List<Int> = emptyList(),
)
