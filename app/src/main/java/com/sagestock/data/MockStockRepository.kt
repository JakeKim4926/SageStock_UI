package com.sagestock.data

import android.content.Context
import com.sagestock.domain.Candle
import com.sagestock.domain.IndicatorSet
import com.sagestock.domain.Market
import com.sagestock.domain.Quote
import com.sagestock.domain.Result
import com.sagestock.domain.Stock
import com.sagestock.domain.StockRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockStockRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : StockRepository {

    private val allStocks: List<Stock> by lazy { loadStocks() }

    override suspend fun search(query: String): Result<List<Stock>> {
        delay(300)
        if (query.isBlank()) return Result.Success(emptyList())
        val q = query.lowercase()
        val matched = allStocks.filter {
            it.ticker.lowercase().contains(q) || it.name.lowercase().contains(q)
        }
        return Result.Success(matched)
    }

    override suspend fun getQuote(ticker: String): Result<Quote> {
        delay(200)
        val stock = allStocks.find { it.ticker == ticker }
            ?: return Result.Error("종목을 찾을 수 없습니다: $ticker")
        val indicators = loadIndicators(ticker) ?: return Result.Error("데이터 없음: $ticker")
        val last = indicators.candles.lastOrNull() ?: return Result.Error("캔들 없음")
        val prev = indicators.candles.dropLast(1).lastOrNull()
        val change = if (prev != null) last.close - prev.close else 0.0
        val changePct = if (prev != null && prev.close != 0.0) change / prev.close * 100 else 0.0
        return Result.Success(
            Quote(
                ticker = ticker,
                price = last.close,
                change = change,
                changePercent = changePct,
                open = last.open,
                high = last.high,
                low = last.low,
                volume = last.volume,
                isDelayed = stock.market == Market.KR,
            )
        )
    }

    override suspend fun getIndicators(ticker: String): Result<IndicatorSet> {
        delay(200)
        return loadIndicators(ticker)
            ?.let { Result.Success(it) }
            ?: Result.Error("지표 데이터 없음: $ticker")
    }

    private fun loadStocks(): List<Stock> {
        val json = context.assets.open("mock/stocks.json").bufferedReader().readText()
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Stock(
                ticker = o.getString("ticker"),
                name = o.getString("name"),
                market = Market.valueOf(o.getString("market")),
                exchange = o.getString("exchange"),
            )
        }
    }

    private fun loadIndicators(ticker: String): IndicatorSet? {
        val file = "mock/indicators_$ticker.json"
        return runCatching {
            val json = context.assets.open(file).bufferedReader().readText()
            val o = JSONObject(json)
            val candlesArr = o.getJSONArray("candles")
            val rsiArr = o.getJSONArray("rsiSeries")
            IndicatorSet(
                ticker = o.getString("ticker"),
                rsi14 = o.getDouble("rsi14"),
                candles = List(candlesArr.length()) { i ->
                    val c = candlesArr.getJSONObject(i)
                    Candle(
                        date = c.getString("date"),
                        open = c.getDouble("open"),
                        high = c.getDouble("high"),
                        low = c.getDouble("low"),
                        close = c.getDouble("close"),
                        volume = c.getLong("volume"),
                    )
                },
                rsiSeries = List(rsiArr.length()) { i -> rsiArr.getDouble(i) },
            )
        }.getOrNull()
    }
}
