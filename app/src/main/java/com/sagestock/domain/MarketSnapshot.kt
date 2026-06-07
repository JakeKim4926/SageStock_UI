package com.sagestock.domain

/** 홈/대시보드용 종목 시세 스냅샷(가격·등락·스파크라인). Mock JSON 또는 백엔드 시장 스냅샷에서 채운다. */
data class StockSnapshot(
    val stock: Stock,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val sparkline: List<Double> = emptyList(),
)

/** 장 상태. 백엔드/시각으로 결정되며 홈 StatusBadge에 매핑. */
enum class MarketStatus { OPEN, PRE_MARKET, AFTER_MARKET, CLOSED }
