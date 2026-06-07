package com.sagestock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagestock.ui.theme.SageStockTheme
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography
import com.sagestock.ui.theme.UpDownPalette

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val palette by viewModel.palette.collectAsStateWithLifecycle()
    SettingsContent(
        palette = palette,
        onTogglePalette = viewModel::togglePalette,
        onLogout = { viewModel.logout(); onLoggedOut() },
    )
}

@Composable
fun SettingsContent(
    palette: UpDownPalette,
    onTogglePalette: () -> Unit,
    onLogout: () -> Unit,
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = dims.screenPadding, vertical = 12.dp)) {
            Text("설정", style = SageTypography.titleMedium, color = c.textPrimary)
            Text("앱 환경 · 데이터 · 표시", style = SageTypography.labelSmall, color = c.textTertiary)
        }
        HorizontalDivider(color = c.line)

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            SettingGroup("계정") {
                SettingRow(title = "로그인 정보", value = "admin")
                HorizontalDivider(color = c.line)
                SettingRow(title = "로그아웃", titleColor = c.up, onClick = onLogout)
            }
            SettingGroup("데이터") {
                SettingRow(title = "데이터 소스 상태", value = "KR·US 정상 · 15분 지연", valueColor = c.positive)
                HorizontalDivider(color = c.line)
                SettingRow(title = "시장 표시", value = "한국 + 미국")
            }
            SettingGroup("차트 기본값") {
                SettingRow(title = "기본 차트 기간", value = "3개월")
                HorizontalDivider(color = c.line)
                SettingRow(title = "기본 보조지표", value = "EMA · RSI · 볼린저")
            }
            SettingGroup("표시") {
                val paletteValue = if (palette == UpDownPalette.KOREA) "빨강▲ / 파랑▼ (한국식)" else "초록▲ / 빨강▼ (미국식)"
                SettingRow(title = "상승/하락 색상", value = paletteValue, onClick = onTogglePalette)
                HorizontalDivider(color = c.line)
                SettingRow(title = "통화 표시", value = "자동 (₩ / $)")
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "SageStock v0.1.0",
                style = SageTypography.labelSmall,
                color = c.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SettingGroup(title: String, content: @Composable () -> Unit) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Text(
        title,
        style = SageTypography.labelMedium,
        color = c.textTertiary,
        modifier = Modifier.padding(horizontal = dims.screenPadding).padding(top = 16.dp, bottom = 8.dp),
    )
    Column(
        Modifier.fillMaxWidth().padding(horizontal = dims.screenPadding)
            .clip(RoundedCornerShape(14.dp)).background(c.surface),
    ) { content() }
}

@Composable
private fun SettingRow(
    title: String,
    value: String? = null,
    titleColor: Color? = null,
    valueColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = SageTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = SageTypography.bodyMedium, color = titleColor ?: c.textPrimary, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, style = SageTypography.bodySmall, color = valueColor ?: c.textSecondary)
        }
        if (onClick != null) {
            Spacer(Modifier.width(6.dp))
            Text("›", style = SageTypography.bodyMedium, color = c.textTertiary)
        }
    }
}

@Preview(showBackground = true, name = "설정")
@Composable
private fun PreviewSettings() = SageStockTheme {
    SettingsContent(palette = UpDownPalette.KOREA, onTogglePalette = {}, onLogout = {})
}
