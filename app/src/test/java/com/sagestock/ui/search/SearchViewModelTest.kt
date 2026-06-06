package com.sagestock.ui.search

import app.cash.turbine.test
import com.sagestock.domain.Market
import com.sagestock.domain.Result
import com.sagestock.domain.Stock
import com.sagestock.domain.StockRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repo: StockRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty results`() = runTest {
        val vm = SearchViewModel(repo)
        vm.uiState.test {
            val initial = awaitItem()
            assertTrue((initial.results as Result.Success).data.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `blank query stays empty without calling repo`() = runTest {
        val vm = SearchViewModel(repo)
        vm.onQueryChange("  ")
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue((state.results as Result.Success).data.isEmpty())
    }

    @Test
    fun `query returns matching stocks after debounce`() = runTest {
        val stocks = listOf(Stock("005930", "삼성전자", Market.KR, "KOSPI"))
        coEvery { repo.search("삼성") } returns Result.Success(stocks)

        val vm = SearchViewModel(repo)
        vm.onQueryChange("삼성")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(stocks, (vm.uiState.value.results as Result.Success).data)
    }

    @Test
    fun `error from repo surfaces as Error state`() = runTest {
        coEvery { repo.search("오류") } returns Result.Error("서버 오류")

        val vm = SearchViewModel(repo)
        vm.onQueryChange("오류")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals("서버 오류", (vm.uiState.value.results as Result.Error).message)
    }
}
