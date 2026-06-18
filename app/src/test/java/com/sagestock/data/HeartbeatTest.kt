package com.sagestock.data

import androidx.lifecycle.LifecycleOwner
import com.sagestock.data.remote.HealthApi
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeartbeatTest {

    private val healthApi: HealthApi = mockk()
    private val session: SessionManager = mockk()
    private val owner: LifecycleOwner = mockk(relaxed = true)

    @Test
    fun `pings on foreground when logged in`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        every { session.accessToken() } returns "token"
        coEvery { healthApi.ping() } just Runs
        val heartbeat = Heartbeat(healthApi, session, dispatcher)

        heartbeat.onStart(owner)
        runCurrent() // 즉시 첫 tick 실행

        coVerify(exactly = 1) { healthApi.ping() }
        heartbeat.onStop(owner)
    }

    @Test
    fun `does not ping when logged out`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        every { session.accessToken() } returns null
        val heartbeat = Heartbeat(healthApi, session, dispatcher)

        heartbeat.onStart(owner)
        runCurrent()

        coVerify(exactly = 0) { healthApi.ping() }
        heartbeat.onStop(owner)
    }

    @Test
    fun `stops pinging after background`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        every { session.accessToken() } returns "token"
        coEvery { healthApi.ping() } just Runs
        val heartbeat = Heartbeat(healthApi, session, dispatcher)

        heartbeat.onStart(owner)
        runCurrent() // 첫 tick (1회)
        heartbeat.onStop(owner)
        advanceTimeBy(30 * 60 * 1000L) // 30분 흘려도 추가 ping 없어야

        coVerify(exactly = 1) { healthApi.ping() }
    }
}
