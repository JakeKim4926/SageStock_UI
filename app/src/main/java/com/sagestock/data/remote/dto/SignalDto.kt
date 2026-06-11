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
