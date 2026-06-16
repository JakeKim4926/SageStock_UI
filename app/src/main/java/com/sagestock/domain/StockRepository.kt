package com.sagestock.domain

interface StockRepository {
    suspend fun search(query: String): Result<List<Stock>>
    suspend fun getMarketSnapshots(): Result<List<StockSnapshot>>
    suspend fun getQuote(ticker: String): Result<Quote>
    suspend fun getIndicators(ticker: String): Result<IndicatorSet>
    suspend fun getSignals(): Result<List<Signal>>
    suspend fun getSignalRanking(): Result<List<SignalScore>>
    suspend fun getPredictions(): Result<List<Prediction>>
}
