package com.sagestock.ui.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagestock.domain.TradeSide
import com.sagestock.ui.components.ButtonTone
import com.sagestock.ui.components.SageButton
import com.sagestock.ui.components.SageTextField
import com.sagestock.ui.components.formatPrice
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

@Composable
fun PaperInputScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: PaperInputViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    PaperInputContent(
        state = state,
        onBack = onBack,
        onSide = viewModel::setSide,
        onPrice = viewModel::setPrice,
        onQuantity = viewModel::setQuantity,
        onRatio = viewModel::applyRatio,
        onSubmit = viewModel::submit,
    )
}

@Composable
fun PaperInputContent(
    state: PaperInputUiState,
    onBack: () -> Unit,
    onSide: (TradeSide) -> Unit,
    onPrice: (String) -> Unit,
    onQuantity: (String) -> Unit,
    onRatio: (Double) -> Unit,
    onSubmit: () -> Unit,
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    val isBuy = state.side == TradeSide.BUY
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = c.textPrimary)
            }
            Text("${state.name} · 가상매매", style = SageTypography.titleMedium, color = c.textPrimary)
        }
        HorizontalDivider(color = c.line)

        SideSegment(side = state.side, onSide = onSide, modifier = Modifier.padding(dims.screenPadding))
        HorizontalDivider(color = c.line)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(dims.screenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("현재가", style = SageTypography.bodySmall, color = c.textSecondary)
                Text(formatPrice(state.market, state.currentPrice), style = SageTypography.bodyMedium, color = c.textPrimary)
            }

            SageTextField(label = "주문 단가", value = state.priceInput, onValueChange = onPrice, placeholder = "단가")
            SageTextField(label = "수량", value = state.quantityInput, onValueChange = onQuantity, placeholder = "0")

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RatioChip("10%") { onRatio(0.10) }
                RatioChip("25%") { onRatio(0.25) }
                RatioChip("50%") { onRatio(0.50) }
                RatioChip("최대") { onRatio(1.0) }
            }

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SummaryRow("주문 금액", "%,d".format(state.orderAmount.toLong()))
                SummaryRow("가상 예수금", "%,d".format(state.cashAfter.toLong()))
            }

            val error = state.error
            if (error != null) {
                Text(error, style = SageTypography.labelMedium, color = c.up)
            }

            SageButton(
                text = if (isBuy) "가상 매수 기록" else "가상 매도 기록",
                onClick = onSubmit,
                tone = if (isBuy) ButtonTone.UP else ButtonTone.DOWN,
                enabled = state.canSubmit,
            )
            Text(
                "실제 주문이 아닙니다 — 로컬에 매매 기록만 저장되며 저장 즉시 포트폴리오에 반영됩니다.",
                style = SageTypography.labelSmall,
                color = c.textTertiary,
            )
        }
    }
}

@Composable
private fun SideSegment(side: TradeSide, onSide: (TradeSide) -> Unit, modifier: Modifier = Modifier) {
    val c = SageTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.surface2).padding(3.dp),
    ) {
        TradeSide.entries.forEach { s ->
            val selected = s == side
            val tone = if (s == TradeSide.BUY) c.up else c.down
            val label = if (s == TradeSide.BUY) "매수" else "매도"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) c.bg else c.surface2)
                    .then(if (selected) Modifier.border(1.dp, tone.copy(alpha = 0.5f), RoundedCornerShape(8.dp)) else Modifier)
                    .clickable { onSide(s) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = SageTypography.bodyMedium, color = if (selected) tone else c.textSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun RatioChip(label: String, onClick: () -> Unit) {
    val c = SageTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, style = SageTypography.labelSmall, color = c.textSecondary)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    val c = SageTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = SageTypography.bodySmall, color = c.textSecondary)
        Text(value, style = SageTypography.bodyMedium, color = c.textPrimary)
    }
}
