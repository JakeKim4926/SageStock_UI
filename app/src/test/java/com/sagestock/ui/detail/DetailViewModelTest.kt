package com.sagestock.ui.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.sagestock.domain.Candle
import com.sagestock.domain.IndicatorSet
import com.sagestock.domain.Quote
import com.sagestock.domain.Result
import com.sagestock.domain.StockRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repo: StockRepository = mockk()

    private val mockQuote = Quote("005930", 78400.0, 1400.0, 1.82, 77200.0, 78900.0, 76800.0, 12400000)
    private val mockIndicators = IndicatorSet(
        ticker = "005930",
        rsi14 = 63.4,
        candles = listOf(Candle("2026-06-06", 77200.0, 78900.0, 76800.0, 78400.0, 12400000)),
        rsiSeries = listOf(63.4),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun savedState(ticker: String = "005930", index: Int = -1) =
        SavedStateHandle(mapOf("ticker" to ticker, "index" to index))

    @Test
    fun `initial state is Loading`() = runTest {
        coEvery { repo.getQuote(any()) } coAnswers { kotlinx.coroutines.delay(1000); Result.Success(mockQuote) }
        coEvery { repo.getIndicators(any()) } coAnswers { kotlinx.coroutines.delay(1000); Result.Success(mockIndicators) }

        val vm = DetailViewModel(repo, savedState())
        vm.uiState.test {
            val first = awaitItem()
            assertTrue(first.quote is Result.Loading)
            assertTrue(first.indicators is Result.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `successful load transitions to Success`() = runTest {
        coEvery { repo.getQuote("005930") } returns Result.Success(mockQuote)
        coEvery { repo.getIndicators("005930") } returns Result.Success(mockIndicators)

        val vm = DetailViewModel(repo, savedState())
        vm.uiState.test {
            awaitItem() // Loading

            advanceUntilIdle()

            val success = awaitItem()
            assertEquals(mockQuote, (success.quote as Result.Success).data)
            assertEquals(mockIndicators, (success.indicators as Result.Success).data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `quote error is exposed in state`() = runTest {
        coEvery { repo.getQuote("005930") } returns Result.Error("종목 없음")
        coEvery { repo.getIndicators("005930") } returns Result.Success(mockIndicators)

        val vm = DetailViewModel(repo, savedState())
        vm.uiState.test {
            awaitItem() // Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("종목 없음", (state.quote as Result.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry reloads data`() = runTest {
        coEvery { repo.getQuote("005930") } returnsMany listOf(
            Result.Error("일시 오류"),
            Result.Success(mockQuote),
        )
        coEvery { repo.getIndicators("005930") } returns Result.Success(mockIndicators)

        val vm = DetailViewModel(repo, savedState())
        vm.uiState.test {
            awaitItem()
            advanceUntilIdle()
            val firstLoad = awaitItem()
            assertTrue(firstLoad.quote is Result.Error)

            vm.retry()
            awaitItem() // Loading again
            advanceUntilIdle()

            val retried = awaitItem()
            assertTrue(retried.quote is Result.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleEma flips showEma in config`() = runTest {
        coEvery { repo.getQuote(any()) } returns Result.Success(mockQuote)
        coEvery { repo.getIndicators(any()) } returns Result.Success(mockIndicators)

        val vm = DetailViewModel(repo, savedState())
        advanceUntilIdle()

        assertFalse(vm.uiState.value.config.showEma)
        vm.toggleEma()
        assertTrue(vm.uiState.value.config.showEma)
        vm.toggleEma()
        assertFalse(vm.uiState.value.config.showEma)
    }

    @Test
    fun `highlightIndex is read from SavedStateHandle`() = runTest {
        coEvery { repo.getQuote(any()) } returns Result.Success(mockQuote)
        coEvery { repo.getIndicators(any()) } returns Result.Success(mockIndicators)

        val vm = DetailViewModel(repo, savedState(index = 3))
        assertEquals(3, vm.uiState.value.highlightIndex)
    }
}
