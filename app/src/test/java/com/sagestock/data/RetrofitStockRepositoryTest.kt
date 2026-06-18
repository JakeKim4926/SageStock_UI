package com.sagestock.data

import com.sagestock.data.remote.SageStockApi
import com.sagestock.domain.CrossType
import com.sagestock.domain.Market
import com.sagestock.domain.Result
import com.sagestock.domain.SignalType
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class RetrofitStockRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RetrofitStockRepository
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SageStockApi::class.java)
        repository = RetrofitStockRepository(
            api = api,
            mock = mockk(),
            json = json,
            io = UnconfinedTestDispatcher(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `search maps StockDto list to domain`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"ticker":"005930","name":"삼성전자","market":"KR","exchange":"KOSPI"}]""",
            ),
        )

        val result = repository.search("삼성")

        assertTrue(result is Result.Success)
        val stocks = (result as Result.Success).data
        assertEquals(1, stocks.size)
        assertEquals("005930", stocks[0].ticker)
        assertEquals(Market.KR, stocks[0].market)
    }

    @Test
    fun `blank query short-circuits without hitting network`() = runTest {
        val result = repository.search("   ")

        assertTrue(result is Result.Success)
        assertEquals(0, (result as Result.Success).data.size)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `getQuote maps fields and enum`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"ticker":"005930","price":73500,"change":1200,"changePercent":1.66,
                   "open":72300,"high":73800,"low":72100,"volume":12345678,
                   "isDelayed":true,"market":"KR"}""",
            ),
        )

        val result = repository.getQuote("005930")

        assertTrue(result is Result.Success)
        val quote = (result as Result.Success).data
        assertEquals(73500.0, quote.price, 0.0)
        assertEquals(true, quote.isDelayed)
        assertEquals(Market.KR, quote.market)
    }

    @Test
    fun `getIndicators maps candles and cross markers`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"ticker":"005930","rsi14":58.3,
                   "candles":[{"date":"2026-06-05","open":72300,"high":73800,"low":72100,"close":73500,"volume":12345678}],
                   "rsiSeries":[55.1,58.3],
                   "crossMarkers":[{"index":0,"type":"GOLDEN"}],
                   "divergenceMarkers":[0]}""",
            ),
        )

        val result = repository.getIndicators("005930", "1w", "1y")

        assertTrue(result is Result.Success)
        val set = (result as Result.Success).data
        assertEquals(1, set.candles.size)
        assertEquals(73500.0, set.candles[0].close, 0.0)
        assertEquals(CrossType.GOLDEN, set.crossMarkers[0].type)
        // interval/range가 쿼리로 붙는지 확인
        val path = server.takeRequest().path
        assertTrue(path!!.contains("interval=1w"))
        assertTrue(path.contains("range=1y"))
    }

    @Test
    fun `getSignals maps signal enum fields`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[{"id":"sig_001","ticker":"005930","stockName":"삼성전자","type":"GOLDEN_CROSS",
                   "date":"2026-06-05","description":"5일선이 20일선을 상향 돌파","riskLevel":"MEDIUM","candleIndex":42}]""",
            ),
        )

        val result = repository.getSignals()

        assertTrue(result is Result.Success)
        val signals = (result as Result.Success).data
        assertEquals(SignalType.GOLDEN_CROSS, signals[0].type)
        assertEquals(42, signals[0].candleIndex)
    }

    @Test
    fun `error body with code and message maps to Result Error`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(404).setBody(
                """{"code":"STOCK_NOT_FOUND","message":"종목을 찾을 수 없습니다: 000000"}""",
            ),
        )

        val result = repository.getQuote("000000")

        assertTrue(result is Result.Error)
        assertEquals("종목을 찾을 수 없습니다: 000000", (result as Result.Error).message)
    }

    @Test
    fun `FastAPI detail array error maps to first msg`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(422).setBody(
                """{"detail":[{"loc":["query","q"],"msg":"field required","type":"value_error.missing"}]}""",
            ),
        )

        val result = repository.getIndicators("005930", "1d", "6m")

        assertTrue(result is Result.Error)
        assertEquals("field required", (result as Result.Error).message)
    }
}
