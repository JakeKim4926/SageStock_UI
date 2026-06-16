package com.sagestock.domain

import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun observeWatchlist(): Flow<List<Stock>>
    suspend fun toggle(stock: Stock)
    suspend fun remove(ticker: String)
    suspend fun clearAll()
}
