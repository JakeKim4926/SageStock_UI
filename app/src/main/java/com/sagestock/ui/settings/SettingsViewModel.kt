package com.sagestock.ui.settings

import androidx.lifecycle.ViewModel
import com.sagestock.data.SessionManager
import com.sagestock.data.SettingsManager
import com.sagestock.ui.theme.UpDownPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val sessionManager: SessionManager,
) : ViewModel() {

    val palette: StateFlow<UpDownPalette> = settingsManager.palette

    /** 한국식(상승 빨강) ↔ 미국식(상승 초록) 전환 — 앱 전체 등락색 즉시 반영. */
    fun togglePalette() {
        val next = if (palette.value == UpDownPalette.KOREA) UpDownPalette.US else UpDownPalette.KOREA
        settingsManager.setPalette(next)
    }

    fun logout() = sessionManager.logout()
}
