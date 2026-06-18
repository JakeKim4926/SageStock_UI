package com.sagestock.data

import android.content.Context
import com.sagestock.domain.Candle
import com.sagestock.domain.CrossMarker
import com.sagestock.domain.CrossType
import com.sagestock.domain.IndicatorSet
import com.sagestock.domain.Market
import com.sagestock.domain.Prediction
import com.sagestock.domain.PredictionStatus
import com.sagestock.domain.Quote
import com.sagestock.domain.Result
import com.sagestock.domain.RiskLevel
import com.sagestock.domain.Signal
import com.sagestock.domain.SignalScore
import com.sagestock.domain.SignalType
import com.sagestock.domain.Stock
import com.sagestock.domain.StockRepository
import com.sagestock.domain.StockSnapshot
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

    override suspend fun getMarketSnapshots(): Result<List<StockSnapshot>> {
        delay(200)
        return withContext(io) { Result.Success(loadSnapshots()) }
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

    // interval/range는 서버 집계 파라미터(api-spec). Mock은 무시하고 원본 일봉 시리즈를 그대로 반환한다.
    override suspend fun getIndicators(ticker: String, interval: String, range: String): Result<IndicatorSet> {
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

    override suspend fun getSignalRanking(): Result<List<SignalScore>> {
        delay(150)
        return withContext(io) {
            val byTicker = allStocks.associateBy { it.ticker }
            val ranking = allSignals.groupBy { it.ticker }.mapNotNull { (ticker, sigs) ->
                val stock = byTicker[ticker] ?: return@mapNotNull null
                val types = sigs.map { it.type }
                val buy = types.filter { it in BUY_SIGNALS }.distinct()
                val sell = types.filter { it in SELL_SIGNALS }.distinct()
                SignalScore(stock, buy.size.toDouble() - sell.size, buy, sell)
            }.sortedByDescending { it.score }
            Result.Success(ranking)
        }
    }

    override suspend fun getPredictions(): Result<List<Prediction>> {
        delay(150)
        return withContext(io) { Result.Success(loadPredictions()) }
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

    private fun loadSnapshots(): List<StockSnapshot> {
        return runCatching {
            val json = context.assets.open("mock/quotes.json").bufferedReader().readText()
            val arr = JSONArray(json)
            val byTicker = allStocks.associateBy { it.ticker }
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val stock = byTicker[o.getString("ticker")] ?: continue
                    val sparkArr = o.optJSONArray("sparkline")
                    val spark = if (sparkArr == null) emptyList() else List(sparkArr.length()) { sparkArr.getDouble(it) }
                    add(
                        StockSnapshot(
                            stock = stock,
                            price = o.getDouble("price"),
                            change = o.getDouble("change"),
                            changePercent = o.getDouble("changePercent"),
                            volume = o.getLong("volume"),
                            sparkline = spark,
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
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

    private fun loadPredictions(): List<Prediction> {
        return runCatching {
            val json = context.assets.open("mock/predictions.json").bufferedReader().readText()
            val arr = JSONArray(json)

            fun stringList(o: JSONObject, key: String): List<String> {
                if (!o.has(key)) return emptyList()
                val a = o.getJSONArray(key)
                return List(a.length()) { a.getString(it) }
            }

            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Prediction(
                    stock = Stock(
                        ticker = o.getString("ticker"),
                        name = o.getString("name"),
                        market = Market.valueOf(o.getString("market")),
                        exchange = o.getString("exchange"),
                    ),
                    status = PredictionStatus.valueOf(o.getString("status")),
                    riseProbability = o.optDouble("riseProbability", 0.0),
                    expectedReturnPercent = o.optDouble("expectedReturnPercent", 0.0),
                    confidence = o.optDouble("confidence", 0.0),
                    reasons = stringList(o, "reasons"),
                    riskFactors = stringList(o, "riskFactors"),
                )
            }
        }.getOrElse { emptyList() }
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

    private companion object {
        // 시그널 방향(api-spec): 크로스·다이버전스·RSI는 매수/매도로 구분, 볼린저 돌파는 방향 미구분이라 제외.
        val BUY_SIGNALS = setOf(SignalType.GOLDEN_CROSS, SignalType.BULLISH_DIVERGENCE, SignalType.RSI_OVERSOLD)
        val SELL_SIGNALS = setOf(SignalType.DEAD_CROSS, SignalType.BEARISH_DIVERGENCE, SignalType.RSI_OVERBOUGHT)
    }
}
