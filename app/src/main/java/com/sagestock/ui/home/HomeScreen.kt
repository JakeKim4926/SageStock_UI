package com.sagestock.ui.home

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagestock.domain.Market
import com.sagestock.domain.MarketStatus
import com.sagestock.domain.Signal
import com.sagestock.domain.SignalType
import com.sagestock.domain.RiskLevel
import com.sagestock.domain.Stock
import com.sagestock.domain.StockSnapshot
import com.sagestock.ui.components.MarketStatusBadge
import com.sagestock.ui.components.MiniSignalCard
import com.sagestock.ui.components.SageSegment
import com.sagestock.ui.components.SnapshotRow
import com.sagestock.ui.components.StatusBadge
import com.sagestock.ui.theme.SageStockTheme
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

@Composable
fun HomeScreen(
    onStockClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onPredictionClick: () -> Unit,
    onSignalsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onStockClick = onStockClick,
        onSearchClick = onSearchClick,
        onPredictionClick = onPredictionClick,
        onSignalsClick = onSignalsClick,
        onWatchFilter = viewModel::setWatchFilter,
        onTopFilter = viewModel::setTopFilter,
        onRemoveWatched = viewModel::removeWatched,
        onClearWatchlist = viewModel::clearWatchlist,
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onStockClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onPredictionClick: () -> Unit,
    onSignalsClick: () -> Unit = {},
    onWatchFilter: (Market?) -> Unit = {},
    onTopFilter: (TopFilter) -> Unit = {},
    onRemoveWatched: (String) -> Unit = {},
    onClearWatchlist: () -> Unit = {},
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Column(Modifier.fillMaxSize().background(c.bg)) {
        // ── 헤더(앱명·날짜·알림) ──────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dims.screenPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("SageStock", style = SageTypography.titleMedium, color = c.textPrimary)
                Text(state.date, style = SageTypography.labelSmall, color = c.textTertiary)
            }
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "종목 검색", tint = c.textPrimary)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Notifications, contentDescription = "알림", tint = c.textPrimary)
            }
        }

        // ── 장상태 배지 ───────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dims.screenPadding, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MarketStatusBadge(Market.KR, state.krStatus)
            MarketStatusBadge(Market.US, state.usStatus)
            StatusBadge(label = "15분 지연", dotColor = null)
        }
        HorizontalDivider(color = c.line)

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.brand)
            }
            return
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            WatchlistSection(state, onStockClick, onWatchFilter, onRemoveWatched, onClearWatchlist)
            HorizontalDivider(color = c.line)
            SignalsSection(state.todaySignals, onSignalsClick, onStockClick)
            HorizontalDivider(color = c.line)
            TopMoversSection(state, onStockClick, onPredictionClick, onTopFilter)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dims.screenPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = SageTypography.titleSmall, color = c.textPrimary, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(action, style = SageTypography.labelSmall, color = c.textTertiary, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistSection(
    state: HomeUiState,
    onStockClick: (String) -> Unit,
    onWatchFilter: (Market?) -> Unit,
    onRemoveWatched: (String) -> Unit,
    onClearWatchlist: () -> Unit,
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    var showClearDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dims.screenPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("관심종목", style = SageTypography.titleSmall, color = c.textPrimary, modifier = Modifier.weight(1f))
        if (state.watchedSnapshots.isNotEmpty()) {
            Text(
                "전체삭제",
                style = SageTypography.labelSmall,
                color = c.textTertiary,
                modifier = Modifier.clickable { showClearDialog = true },
            )
            Spacer(Modifier.width(12.dp))
        }
        Text("더보기 ›", style = SageTypography.labelSmall, color = c.textTertiary)
    }

    SageSegment(
        options = listOf("전체", "한국", "미국"),
        selectedIndex = when (state.watchFilter) { null -> 0; Market.KR -> 1; Market.US -> 2 },
        onSelect = { onWatchFilter(when (it) { 1 -> Market.KR; 2 -> Market.US; else -> null }) },
        modifier = Modifier.padding(horizontal = dims.screenPadding),
    )
    Spacer(Modifier.height(4.dp))
    if (state.watchlist.isEmpty()) {
        Text(
            "검색에서 종목을 추가하면 여기에 표시돼요.",
            style = SageTypography.bodySmall,
            color = c.textTertiary,
            modifier = Modifier.padding(horizontal = dims.screenPadding, vertical = 16.dp),
        )
    } else {
        state.watchlist.forEach { snapshot ->
            key(snapshot.stock.ticker) {
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.Settled) {
                            false
                        } else {
                            onRemoveWatched(snapshot.stock.ticker)
                            true
                        }
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = { SwipeDeleteBackground(dismissState.dismissDirection) },
                ) {
                    SnapshotRow(
                        snapshot = snapshot,
                        onClick = { onStockClick(snapshot.stock.ticker) },
                        modifier = Modifier.background(c.bg),
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = c.bg,
            title = { Text("관심종목 전체삭제", style = SageTypography.titleSmall, color = c.textPrimary) },
            text = {
                Text(
                    "관심종목 ${state.watchedSnapshots.size}개를 모두 삭제할까요?",
                    style = SageTypography.bodySmall,
                    color = c.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { onClearWatchlist(); showClearDialog = false }) {
                    Text("삭제", color = c.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("취소", color = c.textSecondary)
                }
            },
        )
    }
}

@Composable
private fun SwipeDeleteBackground(direction: SwipeToDismissBoxValue) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    Box(
        Modifier.fillMaxSize().background(c.danger).padding(horizontal = dims.screenPadding),
        contentAlignment = alignment,
    ) {
        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = c.onBrand)
    }
}

@Composable
private fun SignalsSection(signals: List<Signal>, onSignalsClick: () -> Unit, onStockClick: (String) -> Unit) {
    if (signals.isEmpty()) return
    val dims = SageTheme.dims
    SectionHeader("오늘의 주요 시그널", "더보기 ›", onSignalsClick)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = dims.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        signals.forEach { signal ->
            MiniSignalCard(signal = signal, onClick = { onStockClick(signal.ticker) })
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun TopMoversSection(
    state: HomeUiState,
    onStockClick: (String) -> Unit,
    onPredictionClick: () -> Unit,
    onTopFilter: (TopFilter) -> Unit,
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Spacer(Modifier.height(4.dp))
    SageSegment(
        options = listOf("상승률 상위", "거래량 상위", "AI 후보"),
        selectedIndex = when (state.topFilter) { TopFilter.RISERS -> 0; TopFilter.VOLUME -> 1; TopFilter.AI -> 2 },
        onSelect = { onTopFilter(when (it) { 1 -> TopFilter.VOLUME; 2 -> TopFilter.AI; else -> TopFilter.RISERS }) },
        modifier = Modifier.padding(horizontal = dims.screenPadding, vertical = 10.dp),
    )
    if (state.topFilter == TopFilter.AI && state.aiCandidates.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onPredictionClick)
                .padding(horizontal = dims.screenPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = c.brand, modifier = Modifier.height(18.dp))
            Text("AI 후보 미리보기", style = SageTypography.bodySmall, color = c.textPrimary)
            Text("모델 연결 시 활성화", style = SageTypography.labelSmall, color = c.textTertiary)
        }
    } else {
        state.topMovers.forEach { snapshot ->
            SnapshotRow(snapshot = snapshot, onClick = { onStockClick(snapshot.stock.ticker) })
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private fun sampleSnapshot(ticker: String, name: String, market: Market, price: Double, pct: Double) =
    StockSnapshot(Stock(ticker, name, market, market.name), price, price * pct / 100, pct, 1_000_000, listOf(price * 0.98, price * 0.99, price))

@Preview(showBackground = true, name = "홈 — 대시보드")
@Composable
private fun PreviewHome() = SageStockTheme {
    HomeContent(
        state = HomeUiState(
            date = "2026.06.05 (금)",
            krStatus = MarketStatus.OPEN,
            usStatus = MarketStatus.PRE_MARKET,
            loading = false,
            snapshots = listOf(
                sampleSnapshot("005930", "삼성전자", Market.KR, 78400.0, 1.82),
                sampleSnapshot("NVDA", "NVIDIA", Market.US, 1204.5, -0.94),
            ),
            watchedSnapshots = listOf(
                sampleSnapshot("005930", "삼성전자", Market.KR, 78400.0, 1.82),
                sampleSnapshot("NVDA", "NVIDIA", Market.US, 1204.5, -0.94),
            ),
            signals = listOf(
                Signal("1", "247540", "에코프로비엠", SignalType.GOLDEN_CROSS, "2026.06.05", "20·60일선 상향 돌파", RiskLevel.LOW),
            ),
        ),
        onStockClick = {},
        onSearchClick = {},
        onPredictionClick = {},
    )
}
