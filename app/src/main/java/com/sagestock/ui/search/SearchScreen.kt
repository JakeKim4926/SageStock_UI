package com.sagestock.ui.search

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sagestock.domain.Market
import com.sagestock.domain.Result
import com.sagestock.domain.Stock
import com.sagestock.ui.theme.SageStockTheme
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

@Composable
fun SearchScreen(
    onStockClick: (String) -> Unit,
    onSignalsClick: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    SearchContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onStockClick = onStockClick,
        onSignalsClick = onSignalsClick,
    )
}

@Composable
fun SearchContent(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onStockClick: (String) -> Unit,
    onSignalsClick: () -> Unit = {},
) {
    val c = SageTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg)) {
        SearchBar(query = state.query, onQueryChange = onQueryChange)
        HorizontalDivider(color = c.line)
        when (val r = state.results) {
            is Result.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.brand)
            }
            is Result.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(r.message, color = c.textSecondary, style = SageTypography.bodyMedium)
            }
            is Result.Success -> {
                if (r.data.isEmpty() && state.query.isNotBlank()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("검색 결과가 없습니다.", color = c.textSecondary, style = SageTypography.bodyMedium)
                    }
                } else {
                    LazyColumn {
                        if (state.query.isBlank()) {
                            item {
                                SignalsFeedRow(onClick = onSignalsClick)
                                HorizontalDivider(color = c.line)
                            }
                        }
                        items(r.data, key = { it.ticker }) { stock ->
                            StockRow(stock = stock, onClick = { onStockClick(stock.ticker) })
                            HorizontalDivider(color = c.line)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    val c = SageTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = "검색", tint = c.textTertiary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = SageTypography.bodyMedium.copy(color = c.textPrimary),
            decorationBox = { inner ->
                if (query.isEmpty()) Text("종목명 · 티커 검색", style = SageTypography.bodyMedium, color = c.textTertiary)
                inner()
            }
        )
    }
}

@Composable
private fun StockRow(stock: Stock, onClick: () -> Unit) {
    val c = SageTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stock.name, style = SageTypography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(6.dp))
                MarketChip(stock.market)
            }
            Spacer(Modifier.height(2.dp))
            Text("${stock.ticker} · ${stock.exchange}", style = SageTypography.labelSmall, color = c.textTertiary)
        }
    }
}

@Composable
private fun SignalsFeedRow(onClick: () -> Unit) {
    val c = SageTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Notifications, contentDescription = null, tint = c.brand, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("매매 시그널 피드", style = SageTypography.titleSmall, color = c.textPrimary)
            Text("크로스·RSI·볼린저 신호 모아보기", style = SageTypography.labelSmall, color = c.textTertiary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
    }
}

@Composable
private fun MarketChip(market: Market) {
    val c = SageTheme.colors
    Text(
        text = market.name,
        style = SageTypography.labelSmall,
        color = c.textTertiary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.surface2)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Preview(showBackground = true, name = "검색 — 결과")
@Composable
private fun PreviewResults() = SageStockTheme {
    SearchContent(
        state = SearchUiState(
            query = "삼성",
            results = Result.Success(listOf(
                Stock("005930", "삼성전자", Market.KR, "KOSPI"),
                Stock("006400", "삼성SDI", Market.KR, "KOSPI"),
            ))
        ),
        onQueryChange = {},
        onStockClick = {},
    )
}

@Preview(showBackground = true, name = "검색 — 로딩")
@Composable
private fun PreviewLoading() = SageStockTheme {
    SearchContent(
        state = SearchUiState(query = "삼성", results = Result.Loading),
        onQueryChange = {},
        onStockClick = {},
    )
}

@Preview(showBackground = true, name = "검색 — 결과 없음")
@Composable
private fun PreviewEmpty() = SageStockTheme {
    SearchContent(
        state = SearchUiState(query = "존재하지않는종목", results = Result.Success(emptyList())),
        onQueryChange = {},
        onStockClick = {},
    )
}
