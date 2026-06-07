package com.sagestock.data

import com.sagestock.data.local.PaperTradeDao
import com.sagestock.data.local.PaperTradeEntity
import com.sagestock.domain.Market
import com.sagestock.domain.PaperRepository
import com.sagestock.domain.PaperTrade
import com.sagestock.domain.TradeSide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaperRepositoryImpl @Inject constructor(
    private val dao: PaperTradeDao,
) : PaperRepository {

    override fun observeTrades(): Flow<List<PaperTrade>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun record(trade: PaperTrade) = dao.insert(trade.toEntity())
}

private fun PaperTradeEntity.toDomain() = PaperTrade(
    id = id,
    ticker = ticker,
    name = name,
    market = Market.valueOf(market),
    side = TradeSide.valueOf(side),
    price = price,
    quantity = quantity,
    timestamp = timestamp,
)

private fun PaperTrade.toEntity() = PaperTradeEntity(
    id = 0,
    ticker = ticker,
    name = name,
    market = market.name,
    side = side.name,
    price = price,
    quantity = quantity,
    timestamp = timestamp,
)
