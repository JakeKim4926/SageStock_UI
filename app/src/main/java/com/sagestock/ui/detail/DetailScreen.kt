package com.sagestock.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberCandlestickCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.candlestickSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.sagestock.domain.Candle
import com.sagestock.domain.CrossType
import com.sagestock.domain.IndicatorSet
import com.sagestock.domain.Market
import com.sagestock.domain.Quote
import com.sagestock.domain.Result
import com.sagestock.ui.theme.PriceLargeTextStyle
import com.sagestock.ui.theme.SageStockTheme
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography
import com.sagestock.ui.theme.arrowOf
import com.sagestock.ui.theme.priceColorOf
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DetailScreen(
    ticker: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DetailContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onToggleRsi = viewModel::toggleRsi,
        onToggleEma = viewModel::toggleEma,
        onToggleBollinger = viewModel::toggleBollinger,
        onToggleStochastic = viewModel::toggleStochastic,
        onToggleDisparity = viewModel::toggleDisparity,
    )
}

@Composable
fun DetailContent(
    state: DetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleRsi: () -> Unit = {},
    onToggleEma: () -> Unit = {},
    onToggleBollinger: () -> Unit = {},
    onToggleStochastic: () -> Unit = {},
    onToggleDisparity: () -> Unit = {},
) {
    val c = SageTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopBar(ticker = state.ticker, onBack = onBack)
        HorizontalDivider(color = c.line)

        when {
            state.quote is Result.Loading || state.indicators is Result.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.brand)
                }
            }
            state.quote is Result.Error -> {
                ErrorState(message = (state.quote as Result.Error).message, onRetry = onRetry)
            }
            state.indicators is Result.Error -> {
                ErrorState(message = (state.indicators as Result.Error).message, onRetry = onRetry)
            }
            state.quote is Result.Success && state.indicators is Result.Success -> {
                val quote = (state.quote as Result.Success<Quote>).data
                val indicators = (state.indicators as Result.Success<IndicatorSet>).data
                DetailBody(
                    quote = quote,
                    indicators = indicators,
                    config = state.config,
                    highlightIndex = state.highlightIndex,
                    onToggleRsi = onToggleRsi,
                    onToggleEma = onToggleEma,
                    onToggleBollinger = onToggleBollinger,
                    onToggleStochastic = onToggleStochastic,
                    onToggleDisparity = onToggleDisparity,
                )
            }
        }
    }
}

@Composable
private fun TopBar(ticker: String, onBack: () -> Unit) {
    val c = SageTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = c.textPrimary)
        }
        Text(ticker, style = SageTypography.titleMedium, color = c.textPrimary)
    }
}

@Composable
private fun DetailBody(
    quote: Quote,
    indicators: IndicatorSet,
    config: IndicatorConfig,
    highlightIndex: Int,
    onToggleRsi: () -> Unit,
    onToggleEma: () -> Unit,
    onToggleBollinger: () -> Unit,
    onToggleStochastic: () -> Unit,
    onToggleDisparity: () -> Unit,
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Column(Modifier.padding(dims.screenPadding)) {
            PriceBlock(quote = quote)
        }
        HorizontalDivider(color = c.line)

        Spacer(Modifier.height(12.dp))
        MainChart(indicators = indicators, config = config)

        // ── 서브패널 ──────────────────────────────────────
        if (config.showRsi && indicators.rsiSeries.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            SubPanelLabel("RSI (14)", "현재: ${"%.1f".format(indicators.rsi14)}")
            RsiChart(indicators.rsiSeries)
        }
        if (config.showStochastic && indicators.stochasticK.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            SubPanelLabel("스토캐스틱", "K: ${"%.1f".format(indicators.stochasticK.last())}  D: ${"%.1f".format(indicators.stochasticD.lastOrNull() ?: 0.0)}")
            LineSubChart(indicators.stochasticK, indicators.stochasticD)
        }
        if (config.showDisparity && indicators.disparitySeries.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            SubPanelLabel("이격도 (20)", "${"%.2f".format(indicators.disparitySeries.last())}%")
            BarSubChart(indicators.disparitySeries)
        }

        // ── 크로스 마커 ──────────────────────────────────
        if (indicators.crossMarkers.isNotEmpty() || indicators.divergenceMarkers.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = c.line, modifier = Modifier.padding(horizontal = dims.screenPadding))
            Spacer(Modifier.height(12.dp))
            Column(Modifier.padding(horizontal = dims.screenPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("감지된 신호", style = SageTypography.titleSmall, color = c.textPrimary)
                indicators.crossMarkers.forEach { marker ->
                    val (label, color) = when (marker.type) {
                        CrossType.GOLDEN -> "▲ 골든크로스" to SageTheme.price.up
                        CrossType.DEAD   -> "▼ 데드크로스" to SageTheme.price.down
                    }
                    val date = indicators.candles.getOrNull(marker.index)?.date ?: ""
                    Text(
                        "$label  $date",
                        style = SageTypography.bodySmall,
                        color = color,
                    )
                }
                indicators.divergenceMarkers.forEach { idx ->
                    val date = indicators.candles.getOrNull(idx)?.date ?: ""
                    Text(
                        "◇ 다이버전스  $date",
                        style = SageTypography.bodySmall,
                        color = c.warning,
                    )
                }
            }
        }

        // ── 지표 토글 ────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = c.line)
        Spacer(Modifier.height(4.dp))
        Text(
            "보조지표",
            style = SageTypography.titleSmall,
            color = c.textSecondary,
            modifier = Modifier.padding(horizontal = dims.screenPadding, vertical = 8.dp),
        )
        IndicatorToggleRow("RSI (14)", "상대강도지수 — 과매수/과매도 판단", config.showRsi, onToggleRsi)
        IndicatorToggleRow("EMA", "5·20·60·120일 지수이동평균 오버레이", config.showEma, onToggleEma)
        IndicatorToggleRow("볼린저 밴드", "20일 기준, 2σ 상·하단 채널", config.showBollinger, onToggleBollinger)
        IndicatorToggleRow("스토캐스틱", "K/D 오실레이터", config.showStochastic, onToggleStochastic)
        IndicatorToggleRow("이격도", "20일 이동평균 대비 괴리율", config.showDisparity, onToggleDisparity)
    }
}

@Composable
private fun SubPanelLabel(title: String, value: String) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dims.screenPadding, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = SageTypography.labelSmall, color = c.textTertiary)
        Spacer(Modifier.width(8.dp))
        Text(value, style = SageTypography.labelSmall, color = c.textSecondary)
    }
}

@Composable
private fun MainChart(indicators: IndicatorSet, config: IndicatorConfig) {
    val candles = indicators.candles
    if (candles.isEmpty()) return

    val producer = remember { CartesianChartModelProducer() }
    val hasOverlay = config.showEma || config.showBollinger

    LaunchedEffect(candles, config.showEma, config.showBollinger) {
        producer.runTransaction {
            candlestickSeries(
                opening = candles.map { it.open.toFloat() },
                closing = candles.map { it.close.toFloat() },
                low = candles.map { it.low.toFloat() },
                high = candles.map { it.high.toFloat() },
            )
            val overlaySeries = buildList<List<Float>> {
                if (config.showEma) {
                    if (indicators.ema5.size == candles.size) add(indicators.ema5.map { it.toFloat() })
                    if (indicators.ema20.size == candles.size) add(indicators.ema20.map { it.toFloat() })
                    if (indicators.ema60.size == candles.size) add(indicators.ema60.map { it.toFloat() })
                    if (indicators.ema120.size == candles.size) add(indicators.ema120.map { it.toFloat() })
                }
                if (config.showBollinger) {
                    if (indicators.bollingerUpper.size == candles.size) add(indicators.bollingerUpper.map { it.toFloat() })
                    if (indicators.bollingerMid.size == candles.size) add(indicators.bollingerMid.map { it.toFloat() })
                    if (indicators.bollingerLower.size == candles.size) add(indicators.bollingerLower.map { it.toFloat() })
                }
            }
            if (overlaySeries.isNotEmpty()) {
                lineSeries { overlaySeries.forEach { s -> series(s) } }
            }
        }
    }

    if (hasOverlay) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberCandlestickCartesianLayer(),
                rememberLineCartesianLayer(),
            ),
            modelProducer = producer,
            scrollState = rememberVicoScrollState(),
            modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 8.dp),
        )
    } else {
        CartesianChartHost(
            chart = rememberCartesianChart(rememberCandlestickCartesianLayer()),
            modelProducer = producer,
            scrollState = rememberVicoScrollState(),
            modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun RsiChart(rsiSeries: List<Double>) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(rsiSeries) {
        producer.runTransaction {
            lineSeries { series(rsiSeries.map { it.toFloat() }) }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(rememberLineCartesianLayer()),
        modelProducer = producer,
        scrollState = rememberVicoScrollState(),
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 8.dp),
    )
}

@Composable
private fun LineSubChart(series1: List<Double>, series2: List<Double>) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(series1, series2) {
        producer.runTransaction {
            lineSeries {
                series(series1.map { it.toFloat() })
                if (series2.size == series1.size) series(series2.map { it.toFloat() })
            }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(rememberLineCartesianLayer()),
        modelProducer = producer,
        scrollState = rememberVicoScrollState(),
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 8.dp),
    )
}

@Composable
private fun BarSubChart(series: List<Double>) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(series) {
        producer.runTransaction {
            lineSeries { series(series.map { it.toFloat() }) }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(rememberLineCartesianLayer()),
        modelProducer = producer,
        scrollState = rememberVicoScrollState(),
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 8.dp),
    )
}

@Composable
private fun IndicatorToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = dims.screenPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = SageTypography.bodyMedium, color = c.textPrimary)
            Text(description, style = SageTypography.labelSmall, color = c.textTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onBrand,
                checkedTrackColor = c.brand,
                uncheckedThumbColor = c.onBrand,
                uncheckedTrackColor = c.line2,
                uncheckedBorderColor = c.line2,
            ),
        )
    }
}

@Composable
private fun PriceBlock(quote: Quote) {
    val c = SageTheme.colors
    val priceColor = priceColorOf(quote.change)
    val arrow = arrowOf(quote.change)
    val isKr = quote.market == Market.KR
    val fmt = if (isKr)
        "₩" + NumberFormat.getNumberInstance(Locale.KOREA).format(quote.price.toLong())
    else
        "$%.2f".format(quote.price)

    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(fmt, style = PriceLargeTextStyle, color = c.textPrimary)
            Spacer(Modifier.width(10.dp))
            Text(
                "$arrow ${"%.2f".format(quote.changePercent)}%",
                style = SageTypography.bodyMedium,
                color = priceColor,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OhlcItem("시가", quote.open, isKr)
            Spacer(Modifier.width(16.dp))
            OhlcItem("고가", quote.high, isKr)
            Spacer(Modifier.width(16.dp))
            OhlcItem("저가", quote.low, isKr)
            Spacer(Modifier.width(16.dp))
            OhlcItem("거래량", quote.volume.toDouble(), false, isVolume = true)
        }
        if (quote.isDelayed) {
            Spacer(Modifier.height(6.dp))
            Text("15분 지연", style = SageTypography.labelSmall, color = c.warning)
        }
    }
}

@Composable
private fun OhlcItem(label: String, value: Double, isKr: Boolean, isVolume: Boolean = false) {
    val c = SageTheme.colors
    val formatted = when {
        isVolume -> "%.1fM".format(value / 1_000_000)
        isKr -> "₩" + NumberFormat.getNumberInstance(Locale.KOREA).format(value.toLong())
        else -> "$%.1f".format(value)
    }
    Column {
        Text(label, style = SageTypography.labelSmall, color = c.textTertiary)
        Text(formatted, style = SageTypography.bodySmall, color = c.textPrimary)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val c = SageTheme.colors
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = c.textSecondary, style = SageTypography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("다시 시도") }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private val sampleIndicators = IndicatorSet(
    ticker = "005930",
    rsi14 = 63.4,
    candles = emptyList(),
    rsiSeries = listOf(48.2, 52.7, 55.1, 59.8, 61.3, 63.4),
)

@Preview(showBackground = true, name = "상세 — 정상")
@Composable
private fun PreviewNormal() = SageStockTheme {
    DetailContent(
        state = DetailUiState(
            ticker = "005930",
            quote = Result.Success(Quote("005930", 78400.0, 1400.0, 1.82, 77200.0, 78900.0, 76800.0, 12400000)),
            indicators = Result.Success(sampleIndicators),
        ),
        onBack = {},
        onRetry = {},
    )
}

@Preview(showBackground = true, name = "상세 — 로딩")
@Composable
private fun PreviewLoading() = SageStockTheme {
    DetailContent(
        state = DetailUiState(ticker = "005930", quote = Result.Loading, indicators = Result.Loading),
        onBack = {},
        onRetry = {},
    )
}

@Preview(showBackground = true, name = "상세 — 오류")
@Composable
private fun PreviewError() = SageStockTheme {
    DetailContent(
        state = DetailUiState(ticker = "005930", quote = Result.Error("네트워크 오류"), indicators = Result.Loading),
        onBack = {},
        onRetry = {},
    )
}
