package com.sagestock.ui.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagestock.domain.Holding
import com.sagestock.domain.Market
import com.sagestock.domain.PaperTrade
import com.sagestock.domain.Stock
import com.sagestock.domain.TradeSide
import com.sagestock.ui.components.EmptyState
import com.sagestock.ui.components.formatPrice
import com.sagestock.ui.components.formatSignedPercent
import com.sagestock.ui.theme.PriceTextStyle
import com.sagestock.ui.theme.SageStockTheme
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography
import com.sagestock.ui.theme.priceColorOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaperScreen(
    onStockClick: (String) -> Unit = {},
    viewModel: PaperViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PaperContent(state = state, onTab = viewModel::setTab, onStockClick = onStockClick)
}

@Composable
fun PaperContent(
    state: PaperUiState,
    onTab: (PortfolioTab) -> Unit,
    onStockClick: (String) -> Unit,
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = dims.screenPadding, vertical = 12.dp)) {
            Text("가상매매", style = SageTypography.titleMedium, color = c.textPrimary)
            Text("Paper Trading 포트폴리오", style = SageTypography.labelSmall, color = c.textTertiary)
        }

        when {
            state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.brand) }
            }
            state.isEmpty -> {
                EmptyState(
                    title = "보유 종목이 없어요",
                    desc = "종목 상세 → 가상매매 탭에서 매수를 기록하면 여기 포트폴리오에 모여요.",
                    modifier = Modifier.weight(1f),
                )
            }
            else -> {
                SummaryCard(state)
                PortfolioTabs(state.tab, onTab)
                HorizontalDivider(color = c.line)
                when (state.tab) {
                    PortfolioTab.HOLDINGS -> HoldingsList(state.holdings, onStockClick)
                    PortfolioTab.HISTORY -> HistoryList(state.history)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: PaperUiState) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    val profitColor = if (state.totalProfit >= 0) Color(0xFFFF9A90) else Color(0xFF9DC2F0)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.screenPadding, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.brand)
            .padding(16.dp),
    ) {
        Text("총 평가금액", style = SageTypography.labelSmall, color = c.onBrand.copy(alpha = 0.8f))
        Spacer(Modifier.height(2.dp))
        Text("%,d".format(state.totalValue.toLong()), style = SageTypography.headlineSmall, color = c.onBrand)
        Spacer(Modifier.height(2.dp))
        val sign = if (state.totalProfit >= 0) "+" else ""
        Text(
            "$sign%,d ($sign%.2f%%)".format(state.totalProfit.toLong(), state.totalProfitPercent),
            style = SageTypography.bodySmall,
            color = profitColor,
        )
        Spacer(Modifier.height(6.dp))
        Text("가상 예수금 %,d".format(state.cash.toLong()), style = SageTypography.labelSmall, color = c.onBrand.copy(alpha = 0.7f))
    }
}

@Composable
private fun PortfolioTabs(selected: PortfolioTab, onTab: (PortfolioTab) -> Unit) {
    val c = SageTheme.colors
    Row(Modifier.fillMaxWidth()) {
        listOf(PortfolioTab.HOLDINGS to "보유종목", PortfolioTab.HISTORY to "매매 히스토리").forEach { (tab, label) ->
            val isSel = tab == selected
            Column(
                modifier = Modifier.weight(1f).clickable { onTab(tab) }.padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, style = SageTypography.bodyMedium, color = if (isSel) c.textPrimary else c.textTertiary)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.height(2.dp).fillMaxWidth().padding(horizontal = 40.dp).background(if (isSel) c.brand else Color.Transparent))
            }
        }
    }
}

@Composable
private fun HoldingsList(holdings: List<Holding>, onStockClick: (String) -> Unit) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    LazyColumn {
        items(holdings, key = { it.stock.ticker }) { h ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onStockClick(h.stock.ticker) }
                    .padding(horizontal = dims.screenPadding, vertical = dims.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(h.stock.name, style = SageTypography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text("${h.quantity}주 · 평단 ${formatPrice(h.stock.market, h.avgPrice)}", style = SageTypography.labelSmall, color = c.textTertiary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatPrice(h.stock.market, h.value), style = PriceTextStyle, color = c.textPrimary)
                    Text(formatSignedPercent(h.profitPercent), style = SageTypography.labelSmall, color = priceColorOf(h.profitPercent))
                }
            }
            HorizontalDivider(color = c.line)
        }
    }
}

@Composable
private fun HistoryList(history: List<PaperTrade>) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    val dateFmt = remember { SimpleDateFormat("MM.dd HH:mm", Locale.KOREA) }
    LazyColumn {
        items(history, key = { it.id }) { t ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = dims.screenPadding, vertical = dims.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isBuy = t.side == TradeSide.BUY
                val tone = if (isBuy) SageTheme.price.up else SageTheme.price.down
                Text(
                    if (isBuy) "매수" else "매도",
                    style = SageTypography.labelSmall,
                    color = tone,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(tone.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(t.name, style = SageTypography.bodyMedium, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${t.quantity}주 @ ${formatPrice(t.market, t.price)}", style = SageTypography.labelSmall, color = c.textTertiary)
                }
                Text(dateFmt.format(Date(t.timestamp)), style = SageTypography.labelSmall, color = c.textTertiary)
            }
            HorizontalDivider(color = c.line)
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "포트폴리오 — 보유")
@Composable
private fun PreviewPortfolio() = SageStockTheme {
    PaperContent(
        state = PaperUiState(
            loading = false,
            holdings = listOf(
                Holding(Stock("005930", "삼성전자", Market.KR, "KOSPI"), 10, 74000.0, 78400.0),
            ),
            history = listOf(PaperTrade(1, "005930", "삼성전자", Market.KR, TradeSide.BUY, 74000.0, 10, System.currentTimeMillis())),
            totalValue = 784000.0, totalProfit = 44000.0, totalProfitPercent = 5.9, cash = 9_260_000.0,
        ),
        onTab = {}, onStockClick = {},
    )
}

@Preview(showBackground = true, name = "포트폴리오 — 빈상태")
@Composable
private fun PreviewEmpty() = SageStockTheme {
    PaperContent(state = PaperUiState(loading = false), onTab = {}, onStockClick = {})
}
