package com.sagestock.domain

import kotlinx.coroutines.flow.Flow

interface PaperRepository {
    fun observeTrades(): Flow<List<PaperTrade>>
    suspend fun record(trade: PaperTrade)
}
