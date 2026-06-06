package com.sagestock.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberCandlestickCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.candlestickSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.sagestock.domain.Candle
import com.sagestock.domain.IndicatorSet
import com.sagestock.domain.Market
import com.sagestock.domain.Quote
import com.sagestock.domain.Result
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
    val state by viewModel.uiState.collectAsState()
    DetailContent(state = state, onBack = onBack, onRetry = viewModel::retry)
}

@Composable
fun DetailContent(
    state: DetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
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
                DetailBody(quote = quote, indicators = indicators)
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
private fun DetailBody(quote: Quote, indicators: IndicatorSet) {
    val c = SageTheme.colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        PriceBlock(quote = quote)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = c.line)
        Spacer(Modifier.height(16.dp))
        Text("캔들 차트", style = SageTypography.titleSmall, color = c.textPrimary)
        Spacer(Modifier.height(8.dp))
        CandleChart(indicators.candles)
        Spacer(Modifier.height(16.dp))
        Text("RSI (14)", style = SageTypography.titleSmall, color = c.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            "현재 RSI: ${"%.1f".format(indicators.rsi14)}",
            style = SageTypography.bodyMedium,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(8.dp))
        RsiChart(indicators.rsiSeries)
    }
}

@Composable
private fun PriceBlock(quote: Quote) {
    val c = SageTheme.colors
    val priceColor = priceColorOf(quote.change)
    val arrow = arrowOf(quote.change)
    val isKr = quote.ticker.all { it.isDigit() }
    val fmt = if (isKr)
        NumberFormat.getNumberInstance(Locale.KOREA).format(quote.price.toLong())
    else
        "$%.2f".format(quote.price)

    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(fmt, style = SageTypography.headlineSmall.copy(fontSize = 26.sp), color = c.textPrimary)
            Spacer(Modifier.width(10.dp))
            Text(
                "$arrow ${"%.2f".format(quote.changePercent)}%",
                style = SageTypography.bodyMedium.copy(fontSize = 14.sp),
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
        isKr -> NumberFormat.getNumberInstance(Locale.KOREA).format(value.toLong())
        else -> "$%.1f".format(value)
    }
    Column {
        Text(label, style = SageTypography.labelSmall, color = c.textTertiary)
        Text(formatted, style = SageTypography.bodySmall, color = c.textPrimary)
    }
}

@Composable
private fun CandleChart(candles: List<Candle>) {
    if (candles.isEmpty()) return
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(candles) {
        producer.runTransaction {
            candlestickSeries(
                opening = candles.map { it.open.toFloat() },
                closing = candles.map { it.close.toFloat() },
                low = candles.map { it.low.toFloat() },
                high = candles.map { it.high.toFloat() },
            )
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(rememberCandlestickCartesianLayer()),
        modelProducer = producer,
        scrollState = rememberVicoScrollState(),
        modifier = Modifier.fillMaxWidth().height(160.dp),
    )
}

@Composable
private fun RsiChart(rsiSeries: List<Double>) {
    if (rsiSeries.isEmpty()) return
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
        modifier = Modifier.fillMaxWidth().height(100.dp),
    )
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

@Preview(showBackground = true, name = "상세 — 정상")
@Composable
private fun PreviewNormal() = SageStockTheme {
    DetailContent(
        state = DetailUiState(
            ticker = "005930",
            quote = Result.Success(Quote("005930", 78400.0, 1400.0, 1.82, 77200.0, 78900.0, 76800.0, 12400000)),
            indicators = Result.Success(IndicatorSet("005930", 63.4, emptyList(), emptyList())),
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
