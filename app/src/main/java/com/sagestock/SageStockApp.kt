package com.sagestock

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.sagestock.data.Heartbeat
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SageStockApp : Application() {

    @Inject lateinit var heartbeat: Heartbeat

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(heartbeat)
    }
}
