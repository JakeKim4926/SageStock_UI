package com.sagestock.ui.signal

import app.cash.turbine.test
import com.sagestock.domain.Result
import com.sagestock.domain.RiskLevel
import com.sagestock.domain.Signal
import com.sagestock.domain.SignalType
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repo: StockRepository = mockk()

    private val mockSignals = listOf(
        Signal("1", "005930", "삼성전자", SignalType.GOLDEN_CROSS, "2026-05-09", "골든크로스", RiskLevel.LOW, 1),
        Signal("2", "NVDA", "NVIDIA", SignalType.DEAD_CROSS, "2026-05-23", "데드크로스", RiskLevel.HIGH, 3),
        Signal("3", "005930", "삼성전자", SignalType.RSI_OVERBOUGHT, "2026-06-06", "RSI 과매수", RiskLevel.MEDIUM, 5),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        coEvery { repo.getSignals() } coAnswers { kotlinx.coroutines.delay(1000); Result.Success(mockSignals) }

        val vm = SignalViewModel(repo)
        vm.uiState.test {
            val first = awaitItem()
            assertTrue(first.signals is Result.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads signals on init`() = runTest {
        coEvery { repo.getSignals() } returns Result.Success(mockSignals)

        val vm = SignalViewModel(repo)
        vm.uiState.test {
            awaitItem() // Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals(mockSignals, (state.signals as Result.Success).data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter by type returns matching signals`() = runTest {
        coEvery { repo.getSignals() } returns Result.Success(mockSignals)

        val vm = SignalViewModel(repo)
        advanceUntilIdle()

        vm.setFilter(SignalType.GOLDEN_CROSS)
        val filtered = vm.uiState.value.filtered
        assertEquals(1, filtered.size)
        assertEquals(SignalType.GOLDEN_CROSS, filtered.first().type)
    }

    @Test
    fun `null filter returns all signals`() = runTest {
        coEvery { repo.getSignals() } returns Result.Success(mockSignals)

        val vm = SignalViewModel(repo)
        advanceUntilIdle()

        vm.setFilter(SignalType.DEAD_CROSS)
        vm.setFilter(null)
        assertEquals(mockSignals.size, vm.uiState.value.filtered.size)
    }

    @Test
    fun `error from repo surfaces in state`() = runTest {
        coEvery { repo.getSignals() } returns Result.Error("서버 오류")

        val vm = SignalViewModel(repo)
        vm.uiState.test {
            awaitItem() // Loading
            advanceUntilIdle()

            val state = awaitItem()
            assertEquals("서버 오류", (state.signals as Result.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
