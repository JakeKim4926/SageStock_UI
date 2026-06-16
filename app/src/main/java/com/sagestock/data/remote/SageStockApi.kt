package com.sagestock.data.remote

import com.sagestock.data.remote.dto.IndicatorSetDto
import com.sagestock.data.remote.dto.QuoteDto
import com.sagestock.data.remote.dto.SignalDto
import com.sagestock.data.remote.dto.SignalScoreDto
import com.sagestock.data.remote.dto.StockDto
import com.sagestock.data.remote.dto.StockSnapshotDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * SageStock 백엔드 계약(openapi.json). 경로는 openapi paths 그대로(`v1/...`),
 * base URL은 `BuildConfig.API_BASE_URL`. 시세·지표·시그널(뷰어 데이터) 슬라이스만 정의한다.
 */
interface SageStockApi {

    @GET("v1/stocks/search")
    suspend fun searchStocks(
        @Query("q") query: String,
        @Query("market") market: String? = null,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20,
    ): List<StockDto>

    @GET("v1/stocks/{ticker}/quote")
    suspend fun getQuote(@Path("ticker") ticker: String): QuoteDto

    @GET("v1/stocks/{ticker}/indicators")
    suspend fun getIndicators(@Path("ticker") ticker: String): IndicatorSetDto

    @GET("v1/market/snapshots")
    suspend fun getSnapshots(@Query("market") market: String? = null): List<StockSnapshotDto>

    @GET("v1/signals")
    suspend fun getSignals(
        @Query("market") market: String? = null,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20,
    ): List<SignalDto>

    @GET("v1/signals/ranking")
    suspend fun getSignalRanking(
        @Query("market") market: String? = null,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20,
    ): List<SignalScoreDto>

    @GET("v1/watchlist")
    suspend fun getWatchlist(): List<StockDto>

    @PUT("v1/watchlist/{ticker}")
    suspend fun addWatchlist(@Path("ticker") ticker: String)

    @DELETE("v1/watchlist/{ticker}")
    suspend fun removeWatchlist(@Path("ticker") ticker: String)
}
