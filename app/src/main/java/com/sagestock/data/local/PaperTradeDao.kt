package com.sagestock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperTradeDao {
    @Query("SELECT * FROM paper_trade ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PaperTradeEntity>>

    @Insert
    suspend fun insert(entity: PaperTradeEntity)
}
