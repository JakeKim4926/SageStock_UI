package com.sagestock.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagestock.domain.Market
import com.sagestock.domain.Stock
import com.sagestock.ui.theme.SageStockTheme
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

@Composable
fun HomeScreen(
    onStockClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onPredictionClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onStockClick = onStockClick,
        onSearchClick = onSearchClick,
        onPredictionClick = onPredictionClick,
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onStockClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onPredictionClick: () -> Unit,
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dims.screenPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("SageStock", style = SageTypography.titleMedium, color = c.textPrimary)
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "종목 검색", tint = c.textPrimary)
            }
        }
        HorizontalDivider(color = c.line)

        PredictionPreviewRow(onClick = onPredictionClick)
        HorizontalDivider(color = c.line)

        Text(
            "관심목록",
            style = SageTypography.titleSmall,
            color = c.textPrimary,
            modifier = Modifier.padding(horizontal = dims.screenPadding, vertical = dims.gap),
        )

        if (state.watchlist.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "검색에서 종목을 추가하면 여기에 표시돼요.",
                    color = c.textTertiary,
                    style = SageTypography.bodyMedium,
                )
            }
        } else {
            LazyColumn {
                items(state.watchlist, key = { it.ticker }) { stock ->
                    WatchlistRow(stock = stock, onClick = { onStockClick(stock.ticker) })
                    HorizontalDivider(color = c.line)
                }
            }
        }
    }
}

@Composable
private fun PredictionPreviewRow(onClick: () -> Unit) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = dims.screenPadding, vertical = dims.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = c.brand, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("AI 예측", style = SageTypography.titleSmall, color = c.textPrimary)
            Text("상승확률·예상수익률 미리보기", style = SageTypography.labelSmall, color = c.textTertiary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
    }
}

@Composable
private fun WatchlistRow(stock: Stock, onClick: () -> Unit) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = dims.screenPadding, vertical = dims.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stock.name, style = SageTypography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text("${stock.ticker} · ${stock.exchange}", style = SageTypography.labelSmall, color = c.textTertiary)
        }
        MarketStatusBadge(market = stock.market)
    }
}

@Composable
private fun MarketStatusBadge(market: Market) {
    val c = SageTheme.colors
    val isDelayed = market == Market.KR
    val label = if (isDelayed) "지연" else "실시간"
    val color: Color = if (isDelayed) c.warning else c.positive
    Text(
        text = label,
        style = SageTypography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Preview(showBackground = true, name = "홈 — 관심목록")
@Composable
private fun PreviewWatchlist() = SageStockTheme {
    HomeContent(
        state = HomeUiState(
            watchlist = listOf(
                Stock("005930", "삼성전자", Market.KR, "KOSPI"),
                Stock("NVDA", "NVIDIA", Market.US, "NASDAQ"),
            )
        ),
        onStockClick = {},
        onSearchClick = {},
        onPredictionClick = {},
    )
}

@Preview(showBackground = true, name = "홈 — 빈 관심목록")
@Composable
private fun PreviewEmpty() = SageStockTheme {
    HomeContent(
        state = HomeUiState(watchlist = emptyList()),
        onStockClick = {},
        onSearchClick = {},
        onPredictionClick = {},
    )
}
