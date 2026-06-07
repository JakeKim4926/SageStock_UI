package com.sagestock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WatchlistEntity::class, PaperTradeEntity::class], version = 2)
abstract class SageStockDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun paperTradeDao(): PaperTradeDao
}
