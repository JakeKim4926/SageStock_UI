package com.sagestock.data

import android.content.Context
import com.sagestock.domain.Candle
import com.sagestock.domain.CrossMarker
import com.sagestock.domain.CrossType
import com.sagestock.domain.IndicatorSet
import com.sagestock.domain.Market
import com.sagestock.domain.Quote
import com.sagestock.domain.Result
import com.sagestock.domain.RiskLevel
import com.sagestock.domain.Signal
import com.sagestock.domain.SignalType
import com.sagestock.domain.Stock
import com.sagestock.domain.StockRepository
import com.sagestock.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockStockRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : StockRepository {

    private val allStocks: List<Stock> by lazy { loadStocks() }
    private val allSignals: List<Signal> by lazy { loadSignals() }

    override suspend fun search(query: String): Result<List<Stock>> {
        delay(300)
        if (query.isBlank()) return Result.Success(emptyList())
        val q = query.lowercase()
        val matched = withContext(io) {
            allStocks.filter {
                it.ticker.lowercase().contains(q) || it.name.lowercase().contains(q)
            }
        }
        return Result.Success(matched)
    }

    override suspend fun getQuote(ticker: String): Result<Quote> {
        delay(200)
        return withContext(io) {
            val stock = allStocks.find { it.ticker == ticker }
                ?: return@withContext Result.Error("종목을 찾을 수 없습니다: $ticker")
            val indicators = loadIndicators(ticker) ?: return@withContext Result.Error("데이터 없음: $ticker")
            val last = indicators.candles.lastOrNull() ?: return@withContext Result.Error("캔들 없음")
            val prev = indicators.candles.dropLast(1).lastOrNull()
            val change = if (prev != null) last.close - prev.close else 0.0
            val changePct = if (prev != null && prev.close != 0.0) change / prev.close * 100 else 0.0
            Result.Success(
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
                    market = stock.market,
                )
            )
        }
    }

    override suspend fun getIndicators(ticker: String): Result<IndicatorSet> {
        delay(200)
        return withContext(io) {
            loadIndicators(ticker)
                ?.let { Result.Success(it) }
                ?: Result.Error("지표 데이터 없음: $ticker")
        }
    }

    override suspend fun getSignals(): Result<List<Signal>> {
        delay(150)
        return withContext(io) { Result.Success(allSignals) }
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

            fun doubleList(key: String): List<Double> {
                if (!o.has(key)) return emptyList()
                val arr = o.getJSONArray(key)
                return List(arr.length()) { arr.getDouble(it) }
            }

            val crossArr = if (o.has("crossMarkers")) o.getJSONArray("crossMarkers") else JSONArray()
            val divArr = if (o.has("divergenceMarkers")) o.getJSONArray("divergenceMarkers") else JSONArray()

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
                ema5 = doubleList("ema5"),
                ema20 = doubleList("ema20"),
                ema60 = doubleList("ema60"),
                ema120 = doubleList("ema120"),
                bollingerUpper = doubleList("bollingerUpper"),
                bollingerMid = doubleList("bollingerMid"),
                bollingerLower = doubleList("bollingerLower"),
                disparitySeries = doubleList("disparitySeries"),
                stochasticK = doubleList("stochasticK"),
                stochasticD = doubleList("stochasticD"),
                crossMarkers = List(crossArr.length()) { i ->
                    val c = crossArr.getJSONObject(i)
                    CrossMarker(
                        index = c.getInt("index"),
                        type = CrossType.valueOf(c.getString("type")),
                    )
                },
                divergenceMarkers = List(divArr.length()) { i -> divArr.getInt(i) },
            )
        }.getOrNull()
    }

    private fun loadSignals(): List<Signal> {
        return runCatching {
            val json = context.assets.open("mock/signals.json").bufferedReader().readText()
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Signal(
                    id = o.getString("id"),
                    ticker = o.getString("ticker"),
                    stockName = o.getString("stockName"),
                    type = SignalType.valueOf(o.getString("type")),
                    date = o.getString("date"),
                    description = o.getString("description"),
                    riskLevel = RiskLevel.valueOf(o.getString("riskLevel")),
                    candleIndex = if (o.has("candleIndex")) o.getInt("candleIndex") else -1,
                )
            }
        }.getOrElse { emptyList() }
    }
}
