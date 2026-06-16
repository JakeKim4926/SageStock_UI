package com.sagestock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE ticker = :ticker)")
    suspend fun contains(ticker: String): Boolean

    @Insert
    suspend fun insert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE ticker = :ticker")
    suspend fun deleteByTicker(ticker: String)

    @Query("DELETE FROM watchlist")
    suspend fun clear()
}
