package com.sagestock.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paper_trade")
data class PaperTradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticker: String,
    val name: String,
    val market: String,
    val side: String,
    val price: Double,
    val quantity: Int,
    val timestamp: Long,
)
