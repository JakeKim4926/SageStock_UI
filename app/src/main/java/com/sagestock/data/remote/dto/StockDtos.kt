package com.sagestock.data.remote.dto

import kotlinx.serialization.Serializable

/** openapi `StockResponse` — 종목 기본 메타. */
@Serializable
data class StockDto(
    val ticker: String,
    val name: String,
    val market: String,
    val exchange: String,
)

/** openapi `QuoteResponse` — 현재가/등락/당일 OHLCV. */
@Serializable
data class QuoteDto(
    val ticker: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
    val isDelayed: Boolean,
    val market: String,
)

/** openapi `StockSnapshotResponse` — 홈 피드용 시세 스냅샷. */
@Serializable
data class StockSnapshotDto(
    val stock: StockDto,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val sparkline: List<Double> = emptyList(),
)
