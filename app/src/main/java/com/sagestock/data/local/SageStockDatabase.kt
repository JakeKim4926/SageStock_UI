package com.sagestock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WatchlistEntity::class], version = 1)
abstract class SageStockDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
}
