package com.sagestock.domain

/**
 * 관심종목을 기술지표 시그널 종합점수로 정렬한 랭킹 항목(백엔드 `/v1/signals/ranking`).
 * [score] 양수=매수 우세, 음수=매도 우세, 0=중립/신호없음. 절대범위는 고정이 아니므로
 * 표시는 부호(매수/매도)와 순위 위주로 한다.
 */
data class SignalScore(
    val stock: Stock,
    val score: Double,
    val buySignals: List<SignalType>,
    val sellSignals: List<SignalType>,
)
