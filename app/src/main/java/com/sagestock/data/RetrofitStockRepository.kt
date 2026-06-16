package com.sagestock.data

import com.sagestock.data.remote.SageStockApi
import com.sagestock.data.remote.mapper.toDomain
import com.sagestock.data.remote.safeApiCall
import com.sagestock.di.IoDispatcher
import com.sagestock.domain.IndicatorSet
import com.sagestock.domain.Prediction
import com.sagestock.domain.Quote
import com.sagestock.domain.Result
import com.sagestock.domain.Signal
import com.sagestock.domain.SignalScore
import com.sagestock.domain.Stock
import com.sagestock.domain.StockRepository
import com.sagestock.domain.StockSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 7 실서버 구현([StockRepository]). DI에서 `BuildConfig.USE_MOCK=false`일 때
 * [MockStockRepository] 대신 주입된다(인터페이스 동일 → UI 무수정).
 *
 * 예측(`getPredictions`)은 백엔드 `/predictions` 미구현(openapi 부재, feature-spec B5는 최종 단계)
 * 이라 Mock에 위임한다 — 백엔드 엔드포인트가 생기면 이 메서드만 Retrofit으로 교체한다.
 */
@Singleton
class RetrofitStockRepository @Inject constructor(
    private val api: SageStockApi,
    private val mock: MockStockRepository,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) : StockRepository {

    override suspend fun search(query: String): Result<List<Stock>> {
        if (query.isBlank()) return Result.Success(emptyList())
        return safeApiCall(io, json) { api.searchStocks(query).map { it.toDomain() } }
    }

    override suspend fun getMarketSnapshots(): Result<List<StockSnapshot>> =
        safeApiCall(io, json) { api.getSnapshots().map { it.toDomain() } }

    override suspend fun getQuote(ticker: String): Result<Quote> =
        safeApiCall(io, json) { api.getQuote(ticker).toDomain() }

    override suspend fun getIndicators(ticker: String): Result<IndicatorSet> =
        safeApiCall(io, json) { api.getIndicators(ticker).toDomain() }

    override suspend fun getSignals(): Result<List<Signal>> =
        safeApiCall(io, json) { api.getSignals().map { it.toDomain() } }

    override suspend fun getSignalRanking(): Result<List<SignalScore>> =
        safeApiCall(io, json) { api.getSignalRanking().map { it.toDomain() } }

    override suspend fun getPredictions(): Result<List<Prediction>> = mock.getPredictions()
}
