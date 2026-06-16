package com.sagestock.data

import com.sagestock.data.local.WatchlistDao
import com.sagestock.data.local.WatchlistEntity
import com.sagestock.domain.Market
import com.sagestock.domain.Stock
import com.sagestock.domain.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val dao: WatchlistDao,
) : WatchlistRepository {

    override fun observeWatchlist(): Flow<List<Stock>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun toggle(stock: Stock) {
        if (dao.contains(stock.ticker)) {
            dao.deleteByTicker(stock.ticker)
        } else {
            dao.insert(stock.toEntity())
        }
    }

    override suspend fun remove(ticker: String) = dao.deleteByTicker(ticker)

    override suspend fun clearAll() = dao.clear()
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
