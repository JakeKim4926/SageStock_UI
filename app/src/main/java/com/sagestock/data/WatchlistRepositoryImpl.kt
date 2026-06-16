package com.sagestock.data

import com.sagestock.BuildConfig
import com.sagestock.data.local.WatchlistDao
import com.sagestock.data.local.WatchlistEntity
import com.sagestock.data.remote.SageStockApi
import com.sagestock.data.remote.mapper.toDomain
import com.sagestock.di.IoDispatcher
import com.sagestock.domain.Market
import com.sagestock.domain.Stock
import com.sagestock.domain.WatchlistRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 관심종목 저장소. 로컬 Room을 UI 반응형 캐시로 쓰고, add/remove를 백엔드
 * (`/v1/watchlist/{ticker}` PUT·DELETE)에도 미러링한다. 서버 랭킹(`/v1/signals/ranking`)이
 * 서버측 관심종목으로 계산되므로 로컬만 두면 랭킹이 비는 문제를 막는다.
 * USE_MOCK(목업) 모드에선 백엔드 호출을 건너뛴다.
 */
@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val dao: WatchlistDao,
    private val api: SageStockApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : WatchlistRepository {

    override fun observeWatchlist(): Flow<List<Stock>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun toggle(stock: Stock) {
        if (dao.contains(stock.ticker)) {
            dao.deleteByTicker(stock.ticker)
            pushRemove(stock.ticker)
        } else {
            dao.insert(stock.toEntity())
            pushAdd(stock.ticker)
        }
    }

    override suspend fun remove(ticker: String) {
        dao.deleteByTicker(ticker)
        pushRemove(ticker)
    }

    override suspend fun clearAll() {
        val tickers = dao.getAll().map { it.ticker }
        dao.clear()
        tickers.forEach { pushRemove(it) }
    }

    /** 로컬↔서버 병합: 서버에만 있는 건 로컬에 채우고, 로컬에만 있는 건 서버에 올린다(삭제는 하지 않음). */
    override suspend fun sync() {
        if (BuildConfig.USE_MOCK) return
        val remote = runCatching { withContext(io) { api.getWatchlist().map { it.toDomain() } } }.getOrNull() ?: return
        val local = dao.getAll().map { it.toDomain() }
        val remoteTickers = remote.map { it.ticker }.toSet()
        val localTickers = local.map { it.ticker }.toSet()
        remote.filter { it.ticker !in localTickers }.forEach { dao.insert(it.toEntity()) }
        (localTickers - remoteTickers).forEach { pushAdd(it) }
    }

    private suspend fun pushAdd(ticker: String) {
        if (BuildConfig.USE_MOCK) return
        runCatching { withContext(io) { api.addWatchlist(ticker) } }
    }

    private suspend fun pushRemove(ticker: String) {
        if (BuildConfig.USE_MOCK) return
        runCatching { withContext(io) { api.removeWatchlist(ticker) } }
    }
}

private fun WatchlistEntity.toDomain() = Stock(
    ticker = ticker,
    name = name,
    market = Market.valueOf(market),
    exchange = exchange,
)

private fun Stock.toEntity() = WatchlistEntity(
    ticker = ticker,
    name = name,
    market = market.name,
    exchange = exchange,
    addedAt = System.currentTimeMillis(),
)
