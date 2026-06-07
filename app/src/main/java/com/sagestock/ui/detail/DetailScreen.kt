package com.sagestock.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
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
import com.sagestock.domain.Signal
import com.sagestock.domain.Stock
import com.sagestock.ui.components.EmptyState
import com.sagestock.ui.components.MiniChip
import com.sagestock.ui.components.SageButton
import com.sagestock.ui.components.SageSegment
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
    onPaperTrade: (String) -> Unit = {},
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DetailContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onPaperTrade = onPaperTrade,
        onSelectTab = viewModel::selectTab,
        onSetPeriod = viewModel::setPeriod,
        onSetCandleUnit = viewModel::setCandleUnit,
        onToggleWatch = viewModel::toggleWatch,
        onOpenSettings = viewModel::openSettings,
        onCloseSettings = viewModel::closeSettings,
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
    onPaperTrade: (String) -> Unit = {},
    onSelectTab: (DetailTab) -> Unit = {},
    onSetPeriod: (ChartPeriod) -> Unit = {},
    onSetCandleUnit: (CandleUnit) -> Unit = {},
    onToggleWatch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onCloseSettings: () -> Unit = {},
    onToggleRsi: () -> Unit = {},
    onToggleEma: () -> Unit = {},
    onToggleBollinger: () -> Unit = {},
    onToggleStochastic: () -> Unit = {},
    onToggleDisparity: () -> Unit = {},
) {
    val c = SageTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Header(
            stock = state.stock,
            ticker = state.ticker,
            quote = (state.quote as? Result.Success)?.data,
            isWatched = state.isWatched,
            onBack = onBack,
            onToggleWatch = onToggleWatch,
        )
        HorizontalDivider(color = c.line)

        when {
            state.quote is Result.Loading || state.indicators is Result.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.brand)
                }
            }
            state.quote is Result.Error -> ErrorState((state.quote as Result.Error).message, onRetry)
            state.indicators is Result.Error -> ErrorState((state.indicators as Result.Error).message, onRetry)
            state.quote is Result.Success && state.indicators is Result.Success -> {
                DetailBody(
                    quote = (state.quote as Result.Success<Quote>).data,
                    indicators = (state.indicators as Result.Success<IndicatorSet>).data,
                    state = state,
                    onPaperTrade = onPaperTrade,
                    onSelectTab = onSelectTab,
                    onSetPeriod = onSetPeriod,
                    onSetCandleUnit = onSetCandleUnit,
                    onOpenSettings = onOpenSettings,
                    onToggleRsi = onToggleRsi,
                    onToggleEma = onToggleEma,
                    onToggleBollinger = onToggleBollinger,
                    onToggleStochastic = onToggleStochastic,
                    onToggleDisparity = onToggleDisparity,
                )
            }
        }
    }

    if (state.showSettings && state.indicators is Result.Success) {
        IndicatorSettingsSheet(config = state.config, onClose = onCloseSettings)
    }
}

@Composable
private fun Header(
    stock: Stock?,
    ticker: String,
    quote: Quote?,
    isWatched: Boolean,
    onBack: () -> Unit,
    onToggleWatch: () -> Unit,
) {
    val c = SageTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = c.textPrimary)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stock?.name ?: ticker, style = SageTypography.titleMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (stock != null) {
                    Spacer(Modifier.width(6.dp))
                    MiniChip(stock.market.name)
                }
            }
            Text(
                if (stock != null) "${stock.ticker} · ${stock.exchange}" else ticker,
                style = SageTypography.labelSmall,
                color = c.textTertiary,
            )
        }
        if (quote != null) {
            val delayLabel = if (quote.isDelayed) "15분 지연" else "실시간"
            val delayColor = if (quote.isDelayed) c.warning else c.positive
            Text(
                delayLabel,
                style = SageTypography.labelSmall,
                color = delayColor,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(delayColor.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        IconButton(onClick = onToggleWatch) {
            Icon(
                imageVector = if (isWatched) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = if (isWatched) "관심목록에서 제거" else "관심목록에 추가",
                tint = if (isWatched) c.brand else c.textTertiary,
            )
        }
    }
}

@Composable
private fun DetailBody(
    quote: Quote,
    indicators: IndicatorSet,
    state: DetailUiState,
    onPaperTrade: (String) -> Unit,
    onSelectTab: (DetailTab) -> Unit,
    onSetPeriod: (ChartPeriod) -> Unit,
    onSetCandleUnit: (CandleUnit) -> Unit,
    onOpenSettings: () -> Unit,
    onToggleRsi: () -> Unit,
    onToggleEma: () -> Unit,
    onToggleBollinger: () -> Unit,
    onToggleStochastic: () -> Unit,
    onToggleDisparity: () -> Unit,
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Column(Modifier.padding(dims.screenPadding)) { PriceBlock(quote) }
        HorizontalDivider(color = c.line)

        DetailTabRow(selected = state.selectedTab, onSelect = onSelectTab)
        HorizontalDivider(color = c.line)

        when (state.selectedTab) {
            DetailTab.CHART -> ChartTab(indicators, state, onSetPeriod, onSetCandleUnit)
            DetailTab.INDICATORS -> IndicatorsTab(
                indicators, state.config, onOpenSettings,
                onToggleRsi, onToggleEma, onToggleBollinger, onToggleStochastic, onToggleDisparity,
            )
            DetailTab.SIGNALS -> SignalsTab(state.signals)
            DetailTab.PAPER -> PaperTab(state.ticker, onPaperTrade)
        }
    }
}

@Composable
private fun DetailTabRow(selected: DetailTab, onSelect: (DetailTab) -> Unit) {
    val c = SageTheme.colors
    Row(Modifier.fillMaxWidth()) {
        DetailTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier.weight(1f).clickable { onSelect(tab) }.padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    tab.label,
                    style = SageTypography.bodyMedium,
                    color = if (isSelected) c.textPrimary else c.textTertiary,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.height(2.dp).width(28.dp)
                        .background(if (isSelected) c.brand else androidx.compose.ui.graphics.Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun ChartTab(
    indicators: IndicatorSet,
    state: DetailUiState,
    onSetPeriod: (ChartPeriod) -> Unit,
    onSetCandleUnit: (CandleUnit) -> Unit,
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims

    // 기간 칩
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = dims.screenPadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ChartPeriod.entries.forEach { p ->
            PeriodChip(p.label, p == state.period) { onSetPeriod(p) }
        }
    }
    // 봉 세그먼트
    SageSegment(
        options = CandleUnit.entries.map { it.label },
        selectedIndex = CandleUnit.entries.indexOf(state.candleUnit),
        onSelect = { onSetCandleUnit(CandleUnit.entries[it]) },
        modifier = Modifier.padding(horizontal = dims.screenPadding),
    )
    Spacer(Modifier.height(8.dp))

    // 표시 캔들 = 기간 슬라이스 후 봉 단위 집계. 오버레이는 일봉에서만(주/월 재계산은 백엔드 몫).
    val windowed = if (state.period == ChartPeriod.ALL) indicators.candles else indicators.candles.takeLast(state.period.days)
    val displayCandles = if (state.candleUnit == CandleUnit.DAY) windowed else windowed.chunked(state.candleUnit.groupSize).map { aggregate(it) }
    val overlays = if (state.candleUnit != CandleUnit.DAY) emptyList() else buildList {
        fun List<Double>.win() = if (state.period == ChartPeriod.ALL) this else takeLast(state.period.days)
        if (state.config.showEma) {
            if (indicators.ema5.size == indicators.candles.size) add(indicators.ema5.win())
            if (indicators.ema20.size == indicators.candles.size) add(indicators.ema20.win())
            if (indicators.ema60.size == indicators.candles.size) add(indicators.ema60.win())
            if (indicators.ema120.size == indicators.candles.size) add(indicators.ema120.win())
        }
        if (state.config.showBollinger) {
            if (indicators.bollingerUpper.size == indicators.candles.size) add(indicators.bollingerUpper.win())
            if (indicators.bollingerMid.size == indicators.candles.size) add(indicators.bollingerMid.win())
            if (indicators.bollingerLower.size == indicators.candles.size) add(indicators.bollingerLower.win())
        }
    }
    MainChart(displayCandles, overlays)
    MarkersList(indicators)
}

@Composable
private fun MarkersList(indicators: IndicatorSet) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    if (indicators.crossMarkers.isEmpty() && indicators.divergenceMarkers.isEmpty()) return
    Spacer(Modifier.height(12.dp))
    Column(Modifier.padding(horizontal = dims.screenPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("감지된 신호", style = SageTypography.titleSmall, color = c.textPrimary)
        indicators.crossMarkers.forEach { marker ->
            val (label, color) = when (marker.type) {
                CrossType.GOLDEN -> "▲ 골든크로스" to SageTheme.price.up
                CrossType.DEAD -> "▼ 데드크로스" to SageTheme.price.down
            }
            val date = indicators.candles.getOrNull(marker.index)?.date ?: ""
            Text("$label  $date", style = SageTypography.bodySmall, color = color)
        }
        indicators.divergenceMarkers.forEach { idx ->
            val date = indicators.candles.getOrNull(idx)?.date ?: ""
            Text("◇ 다이버전스  $date", style = SageTypography.bodySmall, color = c.warning)
        }
    }
}

@Composable
private fun IndicatorsTab(
    indicators: IndicatorSet,
    config: IndicatorConfig,
    onOpenSettings: () -> Unit,
    onToggleRsi: () -> Unit,
    onToggleEma: () -> Unit,
    onToggleBollinger: () -> Unit,
    onToggleStochastic: () -> Unit,
    onToggleDisparity: () -> Unit,
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims

    // ⚙ 지표 설정 진입
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSettings).padding(horizontal = dims.screenPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Tune, contentDescription = null, tint = c.brand, modifier = Modifier.height(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("지표 설정", style = SageTypography.bodyMedium, color = c.textPrimary, modifier = Modifier.weight(1f))
        Text("기간·과매수/과매도·σ ›", style = SageTypography.labelSmall, color = c.textTertiary)
    }
    HorizontalDivider(color = c.line)

    // 서브패널(ON 지표)
    if (config.showRsi && indicators.rsiSeries.isNotEmpty()) {
        SubPanelLabel("RSI (14)", "현재: ${"%.1f".format(indicators.rsi14)}")
        RsiChart(indicators.rsiSeries)
    }
    if (config.showStochastic && indicators.stochasticK.isNotEmpty()) {
        SubPanelLabel("스토캐스틱", "K: ${"%.1f".format(indicators.stochasticK.last())}  D: ${"%.1f".format(indicators.stochasticD.lastOrNull() ?: 0.0)}")
        LineSubChart(indicators.stochasticK, indicators.stochasticD)
    }
    if (config.showDisparity && indicators.disparitySeries.isNotEmpty()) {
        SubPanelLabel("이격도 (20)", "${"%.2f".format(indicators.disparitySeries.last())}%")
        BarSubChart(indicators.disparitySeries)
    }

    Spacer(Modifier.height(8.dp))
    Text("보조지표", style = SageTypography.titleSmall, color = c.textSecondary, modifier = Modifier.padding(horizontal = dims.screenPadding, vertical = 8.dp))
    IndicatorToggleRow("EMA", "5·20·60·120일 지수이동평균 오버레이", config.showEma, onToggleEma)
    IndicatorToggleRow("볼린저 밴드", "20일 기준, 2σ 상·하단 채널", config.showBollinger, onToggleBollinger)
    IndicatorToggleRow("RSI (14)", "상대강도지수 — 과매수/과매도 판단", config.showRsi, onToggleRsi)
    IndicatorToggleRow("스토캐스틱", "14,3,3 K/D 오실레이터", config.showStochastic, onToggleStochastic)
    IndicatorToggleRow("이격도", "20일 이동평균 대비 괴리율", config.showDisparity, onToggleDisparity)
}

@Composable
private fun SignalsTab(signals: List<Signal>) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    if (signals.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
            Text("이 종목의 시그널이 아직 없어요.", style = SageTypography.bodyMedium, color = c.textTertiary)
        }
        return
    }
    Column(Modifier.padding(vertical = 6.dp)) {
        signals.forEach { signal ->
            Column(Modifier.fillMaxWidth().padding(horizontal = dims.screenPadding, vertical = dims.cardPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(signal.type.name, style = SageTypography.labelSmall, color = c.textSecondary)
                    Text(signal.date, style = SageTypography.labelSmall, color = c.textTertiary)
                }
                Text(signal.description, style = SageTypography.bodySmall, color = c.textSecondary)
            }
            HorizontalDivider(color = c.line)
        }
    }
}

@Composable
private fun PaperTab(ticker: String, onPaperTrade: (String) -> Unit) {
    EmptyState(
        title = "이 종목 가상매매",
        desc = "가상 매수·매도를 기록하면 가상매매 탭 포트폴리오에 반영돼요.",
        modifier = Modifier.height(220.dp),
        action = {
            SageButton(text = "가상 매수·매도 기록", onClick = { onPaperTrade(ticker) })
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IndicatorSettingsSheet(config: IndicatorConfig, onClose: () -> Unit) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState, containerColor = c.bg) {
        Column(Modifier.fillMaxWidth().padding(horizontal = dims.screenPadding).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("지표 설정", style = SageTypography.titleMedium, color = c.textPrimary)
            SettingsParamRow("EMA 기간", "5 · 20 · 60 · 120")
            SettingsParamRow("RSI", "14 · 과매수 70 / 과매도 30")
            SettingsParamRow("볼린저 밴드", "20일 · 2σ")
            SettingsParamRow("스토캐스틱", "%K 14 · %D 3 · Smooth 3")
            SettingsParamRow("이격도", "20일")
            Text(
                "값 변경/저장은 설정(09)·백엔드 연동에서 제공됩니다.",
                style = SageTypography.labelSmall,
                color = c.textTertiary,
            )
        }
    }
}

@Composable
private fun SettingsParamRow(label: String, value: String) {
    val c = SageTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = SageTypography.bodyMedium, color = c.textPrimary)
        Text(value, style = SageTypography.bodySmall, color = c.textSecondary)
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = SageTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) c.brand else c.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, style = SageTypography.labelSmall, color = if (selected) c.onBrand else c.textSecondary)
    }
}

private fun aggregate(chunk: List<Candle>): Candle = Candle(
    date = chunk.last().date,
    open = chunk.first().open,
    high = chunk.maxOf { it.high },
    low = chunk.minOf { it.low },
    close = chunk.last().close,
    volume = chunk.sumOf { it.volume },
)

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
private fun MainChart(candles: List<Candle>, overlays: List<List<Double>>) {
    if (candles.isEmpty()) return
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(candles, overlays) {
        producer.runTransaction {
            candlestickSeries(
                opening = candles.map { it.open.toFloat() },
                closing = candles.map { it.close.toFloat() },
                low = candles.map { it.low.toFloat() },
                high = candles.map { it.high.toFloat() },
            )
            if (overlays.isNotEmpty()) {
                lineSeries { overlays.forEach { s -> series(s.map { it.toFloat() }) } }
            }
        }
    }
    if (overlays.isNotEmpty()) {
        CartesianChartHost(
            chart = rememberCartesianChart(rememberCandlestickCartesianLayer(), rememberLineCartesianLayer()),
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
        producer.runTransaction { lineSeries { series(rsiSeries.map { it.toFloat() }) } }
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
        producer.runTransaction { lineSeries { series(series.map { it.toFloat() }) } }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(rememberLineCartesianLayer()),
        modelProducer = producer,
        scrollState = rememberVicoScrollState(),
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 8.dp),
    )
}

@Composable
private fun IndicatorToggleRow(label: String, description: String, checked: Boolean, onToggle: () -> Unit) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = dims.screenPadding, vertical = 10.dp),
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
            stock = Stock("005930", "삼성전자", Market.KR, "KOSPI"),
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
