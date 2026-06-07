package com.sagestock.data

import android.content.Context
import androidx.core.content.edit
import com.sagestock.ui.theme.UpDownPalette
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 표시 설정(영속). 등락 팔레트는 StateFlow로 노출해 테마가 런타임에 즉시 반영한다.
 * 차트 기본값은 상세 초기 구성에 쓰인다. Phase 7에서 일부는 백엔드 동기화.
 */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _palette = MutableStateFlow(loadPalette())
    val palette: StateFlow<UpDownPalette> = _palette.asStateFlow()

    fun setPalette(palette: UpDownPalette) {
        prefs.edit { putString(KEY_PALETTE, palette.name) }
        _palette.value = palette
    }

    private fun loadPalette(): UpDownPalette =
        runCatching { UpDownPalette.valueOf(prefs.getString(KEY_PALETTE, UpDownPalette.KOREA.name)!!) }
            .getOrDefault(UpDownPalette.KOREA)

    private companion object {
        const val KEY_PALETTE = "palette"
    }
}
