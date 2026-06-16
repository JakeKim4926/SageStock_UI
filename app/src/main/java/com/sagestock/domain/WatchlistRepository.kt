package com.sagestock.domain

import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun observeWatchlist(): Flow<List<Stock>>
    suspend fun toggle(stock: Stock)
    suspend fun remove(ticker: String)
    suspend fun clearAll()

    /** 로컬 캐시와 백엔드 관심종목을 양방향 병합 동기화(서버 미연동 시 no-op). 앱 시작 시 1회 호출. */
    suspend fun sync()
}
