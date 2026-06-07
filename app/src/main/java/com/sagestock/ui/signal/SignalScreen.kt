package com.sagestock.ui.signal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagestock.domain.Result
import com.sagestock.domain.RiskLevel
import com.sagestock.domain.Signal
import com.sagestock.domain.SignalType
import com.sagestock.ui.theme.SageStockTheme
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

@Composable
fun SignalScreen(
    onSignalClick: (Signal) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: SignalViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SignalContent(
        state = state,
        onBack = onBack,
        onSignalClick = onSignalClick,
        onFilterChange = viewModel::setFilter,
        onRetry = viewModel::retry,
    )
}

@Composable
fun SignalContent(
    state: SignalUiState,
    onSignalClick: (Signal) -> Unit,
    onFilterChange: (SignalType?) -> Unit,
    onRetry: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val c = SageTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg)) {
        // ── 상단 바 ──────────────────────────────────────
        if (onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = c.textPrimary)
                }
                Text("매매 시그널", style = SageTypography.titleMedium, color = c.textPrimary)
            }
        } else {
            // 탭 모드(와이어프레임 06): 뒤로가기 없이 제목+부제.
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("매매 시그널", style = SageTypography.titleMedium, color = c.textPrimary)
                Text("전체 종목 신호 피드", style = SageTypography.labelSmall, color = c.textTertiary)
            }
        }
        HorizontalDivider(color = c.line)

        // ── 필터 칩 ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(label = "전체", selected = state.filter == null, onClick = { onFilterChange(null) })
            SignalType.entries.forEach { type ->
                FilterChip(
                    label = type.displayName(),
                    selected = state.filter == type,
                    onClick = { onFilterChange(type) },
                )
            }
        }
        HorizontalDivider(color = c.line)

        // ── 바디 ──────────────────────────────────────────
        when {
            state.signals is Result.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.brand)
                }
            }
            state.signals is Result.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text((state.signals as Result.Error).message, color = c.textSecondary, style = SageTypography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onRetry) { Text("다시 시도") }
                    }
                }
            }
            state.filtered.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("해당 시그널이 없습니다.", color = c.textTertiary, style = SageTypography.bodyMedium)
                }
            }
            else -> {
                LazyColumn {
                    items(state.filtered, key = { it.id }) { signal ->
                        SignalCard(signal = signal, onClick = { onSignalClick(signal) })
                        HorizontalDivider(color = c.line)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = SageTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) c.brand else c.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            style = SageTypography.labelSmall,
            color = if (selected) c.onBrand else c.textSecondary,
        )
    }
}

@Composable
private fun SignalCard(signal: Signal, onClick: () -> Unit) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = dims.screenPadding, vertical = dims.cardPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 헤더: 종목 + 날짜
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(signal.stockName, style = SageTypography.titleSmall, color = c.textPrimary)
                Text(
                    signal.ticker,
                    style = SageTypography.labelSmall,
                    color = c.textTertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.surface2)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Text(signal.date, style = SageTypography.labelSmall, color = c.textTertiary)
        }

        // 시그널 타입 칩
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(signal.type.chipColor(c).copy(alpha = 0.12f))
                .border(1.dp, signal.type.chipColor(c).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(signal.type.displayName(), style = SageTypography.labelSmall, color = signal.type.chipColor(c))
        }

        // 설명
        Text(signal.description, style = SageTypography.bodySmall, color = c.textSecondary)

        // 위험도 바
        RiskBar(riskLevel = signal.riskLevel)
    }
}

@Composable
private fun RiskBar(riskLevel: RiskLevel) {
    val c = SageTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("위험도", style = SageTypography.labelSmall, color = c.textTertiary)
        repeat(3) { i ->
            val filled = i < riskLevel.barCount
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (filled) riskLevel.color(c) else c.surface2),
            )
        }
        Text(riskLevel.displayName(), style = SageTypography.labelSmall, color = riskLevel.color(c))
    }
}

// ── 헬퍼 확장 ──────────────────────────────────────────────────────────────

private fun SignalType.displayName() = when (this) {
    SignalType.GOLDEN_CROSS       -> "골든크로스"
    SignalType.DEAD_CROSS         -> "데드크로스"
    SignalType.RSI_OVERSOLD       -> "RSI 과매도"
    SignalType.RSI_OVERBOUGHT     -> "RSI 과매수"
    SignalType.BOLLINGER_BREAKOUT -> "볼린저 돌파"
}

@Composable
private fun SignalType.chipColor(c: com.sagestock.ui.theme.SageStockColors): Color = when (this) {
    SignalType.GOLDEN_CROSS       -> SageTheme.price.up
    SignalType.DEAD_CROSS         -> SageTheme.price.down
    SignalType.RSI_OVERSOLD       -> c.positive
    SignalType.RSI_OVERBOUGHT     -> c.warning
    SignalType.BOLLINGER_BREAKOUT -> c.brand
}

private fun RiskLevel.displayName() = when (this) {
    RiskLevel.LOW    -> "낮음"
    RiskLevel.MEDIUM -> "중간"
    RiskLevel.HIGH   -> "높음"
}

@Composable
private fun RiskLevel.color(c: com.sagestock.ui.theme.SageStockColors): Color = when (this) {
    RiskLevel.LOW    -> c.positive
    RiskLevel.MEDIUM -> c.warning
    RiskLevel.HIGH   -> c.up
}

private val RiskLevel.barCount: Int
    get() = when (this) { RiskLevel.LOW -> 1; RiskLevel.MEDIUM -> 2; RiskLevel.HIGH -> 3 }

// ── Previews ──────────────────────────────────────────────────────────────────

private val sampleSignals = listOf(
    Signal("1", "005930", "삼성전자", SignalType.GOLDEN_CROSS, "2026-05-09", "EMA5이 EMA20을 상향 돌파.", RiskLevel.LOW, 1),
    Signal("2", "NVDA", "NVIDIA", SignalType.DEAD_CROSS, "2026-05-23", "EMA5이 EMA20을 하향 돌파.", RiskLevel.HIGH, 3),
    Signal("3", "005930", "삼성전자", SignalType.RSI_OVERBOUGHT, "2026-06-06", "RSI(14) 63.4 — 과매수.", RiskLevel.MEDIUM, 5),
)

@Preview(showBackground = true, name = "시그널 — 전체")
@Composable
private fun PreviewSignals() = SageStockTheme {
    SignalContent(
        state = SignalUiState(signals = Result.Success(sampleSignals), filter = null),
        onBack = {},
        onSignalClick = {},
        onFilterChange = {},
        onRetry = {},
    )
}

@Preview(showBackground = true, name = "시그널 — 로딩")
@Composable
private fun PreviewLoading() = SageStockTheme {
    SignalContent(
        state = SignalUiState(signals = Result.Loading),
        onBack = {},
        onSignalClick = {},
        onFilterChange = {},
        onRetry = {},
    )
}
