package com.sagestock.data

import com.sagestock.data.remote.HealthApi
import com.sagestock.di.IoDispatcher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Render 무료 인스턴스 슬립(15분 무요청 → 슬립) 방지용 heartbeat.
 * 앱이 **포그라운드(ON_START~ON_STOP) + 로그인 상태**일 때만 [HealthApi.ping]을 10분 주기로 호출한다.
 * 백그라운드/로그아웃이면 중단해 배터리·무료 instance-hours 예산을 태우지 않는다.
 *
 * [com.sagestock.SageStockApp]에서 `ProcessLifecycleOwner`에 옵저버로 등록된다.
 */
@Singleton
class Heartbeat @Inject constructor(
    private val healthApi: HealthApi,
    private val session: SessionManager,
    @IoDispatcher io: CoroutineDispatcher,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + io)
    private var job: Job? = null

    override fun onStart(owner: LifecycleOwner) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                pingIfLoggedIn()
                delay(INTERVAL_MS)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        job?.cancel()
        job = null
    }

    /** 로그인 상태일 때만 ping. 실패는 무시(best-effort). */
    internal suspend fun pingIfLoggedIn() {
        if (session.accessToken() == null) return
        runCatching { healthApi.ping() }
    }

    private companion object {
        const val INTERVAL_MS = 10 * 60 * 1000L
    }
}
