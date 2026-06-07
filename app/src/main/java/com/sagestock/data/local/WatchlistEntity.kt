package com.sagestock.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val ticker: String,
    val name: String,
    val market: String,
    val exchange: String,
    val addedAt: Long,
)
