package com.sagestock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sagestock.ui.components.ButtonTone
import com.sagestock.ui.components.EmptyState
import com.sagestock.ui.components.SageButton
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

/**
 * Phase 1 스텁 — 세션 로그아웃 경로만 제공. 와이어프레임 09 상세(그룹 리스트·차트 기본값·팔레트)는 Phase 6.
 */
@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val c = SageTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("설정", style = SageTypography.titleMedium, color = c.textPrimary)
            Text("앱 환경 · 데이터 · 테마", style = SageTypography.labelSmall, color = c.textTertiary)
        }
        HorizontalDivider(color = c.line)
        EmptyState(
            title = "설정은 준비 중입니다",
            desc = "차트 기본값·등락 색상·데이터 소스 설정이 곧 추가돼요.",
            modifier = Modifier.weight(1f),
            action = {
                SageButton(
                    text = "로그아웃",
                    tone = ButtonTone.OUTLINE,
                    onClick = {
                        viewModel.logout()
                        onLoggedOut()
                    },
                )
            },
        )
    }
}
