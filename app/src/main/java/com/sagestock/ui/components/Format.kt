package com.sagestock.ui.components

import com.sagestock.domain.Market
import com.sagestock.ui.theme.arrowOf
import kotlin.math.abs

/** KR은 정수 원화+천단위 콤마, US는 $ + 소수점 1자리. (핸드오프 통화 규칙) */
fun formatPrice(market: Market, price: Double): String =
    if (market == Market.KR) "%,d".format(price.toLong()) else "$%,.1f".format(price)

/** 등락률: 기호(▲▼) + 절댓값%. 색은 priceColorOf와 병행(접근성). */
fun formatSignedPercent(percent: Double): String =
    "${arrowOf(percent)} ${"%.2f".format(abs(percent))}%"
