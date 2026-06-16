package com.sagestock.data.remote.dto

import kotlinx.serialization.Serializable

/** openapi `SignalResponse` — 지표 기반 탐지 시그널. */
@Serializable
data class SignalDto(
    val id: String,
    val ticker: String,
    val stockName: String,
    val type: String,
    val date: String,
    val description: String,
    val riskLevel: String,
    val candleIndex: Int = -1,
)

/** openapi `SignalScore` — 관심종목 시그널 종합점수 랭킹(`/v1/signals/ranking`). */
@Serializable
data class SignalScoreDto(
    val stock: StockDto,
    val score: Double,
    val buySignals: List<String> = emptyList(),
    val sellSignals: List<String> = emptyList(),
)
