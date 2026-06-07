package com.sagestock.ui.prediction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import com.sagestock.domain.Market
import com.sagestock.domain.Prediction
import com.sagestock.domain.PredictionStatus
import com.sagestock.domain.Result
import com.sagestock.domain.Stock
import com.sagestock.ui.components.ButtonTone
import com.sagestock.ui.components.EmptyState
import com.sagestock.ui.components.SageButton
import com.sagestock.ui.theme.SageStockTheme
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography
import com.sagestock.ui.theme.arrowOf
import com.sagestock.ui.theme.priceColorOf
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PredictionScreen(
    onBack: () -> Unit,
    viewModel: PredictionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PredictionContent(
        state = state,
        onBack = onBack,
        onWatchlistToggle = viewModel::onWatchlistToggle,
        onRetry = viewModel::retry,
    )
}

@Composable
fun PredictionContent(
    state: PredictionUiState,
    onBack: () -> Unit,
    onWatchlistToggle: (Stock) -> Unit,
    onRetry: () -> Unit,
) {
    val c = SageTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg)) {
        // ── 상단 바 ──────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = c.textPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text("AI 급등주 예측", style = SageTypography.titleMedium, color = c.textPrimary)
                Text("베타 · 모델 v0", style = SageTypography.labelSmall, color = c.textTertiary)
            }
            ExperimentalTag()
        }
        HorizontalDivider(color = c.line)

        // ── 바디 ──────────────────────────────────────────
        when (val predictions = state.predictions) {
            is Result.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.brand)
                }
            }
            is Result.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(predictions.message, color = c.textSecondary, style = SageTypography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onRetry) { Text("다시 시도") }
                    }
                }
            }
            is Result.Success -> {
                if (predictions.data.none { it.status == PredictionStatus.READY }) {
                    PredictionEmptyState()
                } else {
                    LazyColumn {
                        items(predictions.data, key = { it.stock.ticker }) { prediction ->
                            PredictionCard(
                                prediction = prediction,
                                isWatched = prediction.stock.ticker in state.watchedTickers,
                                onToggleWatch = { onWatchlistToggle(prediction.stock) },
                            )
                            HorizontalDivider(color = c.line)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExperimentalTag() {
    val c = SageTheme.colors
    Text(
        "실험적",
        style = SageTypography.labelSmall,
        color = c.warning,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.warning.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun PredictionEmptyState() {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Column(Modifier.fillMaxSize()) {
        EmptyState(
            title = "AI 모델 준비중",
            desc = "아직 예측 모델이 연결되지 않았어요.\n연결되면 급등 후보와 근거를 여기서 바로 확인할 수 있어요.",
            modifier = Modifier.weight(1f),
            action = { SageButton(text = "연결 상태 확인", tone = ButtonTone.OUTLINE, onClick = {}) },
        )
        HorizontalDivider(color = c.line)
        Column(Modifier.padding(dims.screenPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EmptyStatusNote("데이터 부족", "학습용 가격 이력이 60일 미만", c.warning)
            EmptyStatusNote("분석 불가", "거래정지·상장폐지 종목", SageTheme.price.down)
        }
    }
}

@Composable
private fun EmptyStatusNote(tag: String, desc: String, tone: Color) {
    val c = SageTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            tag,
            style = SageTypography.labelSmall,
            color = tone,
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(tone.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp),
        )
        Text(desc, style = SageTypography.labelSmall, color = c.textTertiary)
    }
}

@Composable
private fun PredictionCard(prediction: Prediction, isWatched: Boolean, onToggleWatch: () -> Unit) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.screenPadding, vertical = dims.cardPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 헤더: 종목 + 관심목록 토글
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(prediction.stock.name, style = SageTypography.titleSmall, color = c.textPrimary)
                Text(
                    prediction.stock.ticker,
                    style = SageTypography.labelSmall,
                    color = c.textTertiary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.surface2)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            IconButton(onClick = onToggleWatch) {
                Icon(
                    imageVector = if (isWatched) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = if (isWatched) "관심목록에서 제거" else "관심목록에 추가",
                    tint = if (isWatched) c.brand else c.textTertiary,
                )
            }
        }

        if (prediction.status == PredictionStatus.READY) {
            PredictionMetrics(prediction)
        } else {
            Text(prediction.status.guidanceMessage(), style = SageTypography.bodySmall, color = c.textTertiary)
        }
    }
}

@Composable
private fun PredictionMetrics(prediction: Prediction) {
    val c = SageTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        MetricItem("상승확률", "${(prediction.riseProbability * 100).roundToInt()}%", c.textPrimary)
        MetricItem(
            label = "예상상승률",
            value = "${arrowOf(prediction.expectedReturnPercent)} ${"%.1f".format(abs(prediction.expectedReturnPercent))}%",
            color = priceColorOf(prediction.expectedReturnPercent),
        )
        MetricItem("신뢰도", "${(prediction.confidence * 100).roundToInt()}%", c.textPrimary)
    }
    if (prediction.reasons.isNotEmpty()) {
        ReasonList(title = "근거 지표", items = prediction.reasons, dotColor = c.positive)
    }
    if (prediction.riskFactors.isNotEmpty()) {
        ReasonList(title = "리스크 요인", items = prediction.riskFactors, dotColor = c.warning)
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    val c = SageTheme.colors
    Column {
        Text(label, style = SageTypography.labelSmall, color = c.textTertiary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = SageTypography.titleSmall, color = color)
    }
}

@Composable
private fun ReasonList(title: String, items: List<String>, dotColor: Color) {
    val c = SageTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = SageTypography.labelSmall, color = c.textTertiary)
        items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("•", style = SageTypography.bodySmall, color = dotColor)
                Text(item, style = SageTypography.bodySmall, color = c.textSecondary)
            }
        }
    }
}

private fun PredictionStatus.guidanceMessage(): String = when (this) {
    PredictionStatus.READY -> ""
    PredictionStatus.PREPARING -> "AI 예측 모델을 준비하고 있어요. 연결되면 이 카드에 예측 결과가 표시돼요."
    PredictionStatus.INSUFFICIENT_DATA -> "분석에 필요한 거래 데이터가 부족해요."
    PredictionStatus.UNAVAILABLE -> "거래정지 등의 사유로 이 종목은 예측을 제공할 수 없어요."
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val samplePredictions = listOf(
    Prediction(
        stock = Stock("005930", "삼성전자", Market.KR, "KOSPI"),
        status = PredictionStatus.READY,
        riseProbability = 0.68,
        expectedReturnPercent = 3.2,
        confidence = 0.74,
        reasons = listOf("EMA5·EMA20 골든크로스 발생", "RSI(14) 52 — 중립 구간 회복"),
        riskFactors = listOf("반도체 업황 둔화 우려"),
    ),
    Prediction(
        stock = Stock("NVDA", "NVIDIA", Market.US, "NASDAQ"),
        status = PredictionStatus.READY,
        riseProbability = 0.61,
        expectedReturnPercent = -1.4,
        confidence = 0.69,
        reasons = listOf("볼린저 밴드 상단 접근"),
        riskFactors = listOf("밸류에이션 부담"),
    ),
    Prediction(stock = Stock("247540", "에코프로비엠", Market.KR, "KOSDAQ"), status = PredictionStatus.INSUFFICIENT_DATA),
    Prediction(stock = Stock("TSLA", "Tesla", Market.US, "NASDAQ"), status = PredictionStatus.PREPARING),
)

@Preview(showBackground = true, name = "AI 예측 — 목록")
@Composable
private fun PreviewPredictions() = SageStockTheme {
    PredictionContent(
        state = PredictionUiState(predictions = Result.Success(samplePredictions), watchedTickers = setOf("005930")),
        onBack = {},
        onWatchlistToggle = {},
        onRetry = {},
    )
}

@Preview(showBackground = true, name = "AI 예측 — 로딩")
@Composable
private fun PreviewLoading() = SageStockTheme {
    PredictionContent(
        state = PredictionUiState(predictions = Result.Loading),
        onBack = {},
        onWatchlistToggle = {},
        onRetry = {},
    )
}
