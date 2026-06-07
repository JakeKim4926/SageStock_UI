package com.sagestock.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.sagestock.ui.components.MiniChip
import com.sagestock.ui.components.SageSegment
import com.sagestock.ui.theme.SageStockTheme
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

@Composable
fun SearchScreen(
    onStockClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onStockClick = { stock -> viewModel.addRecent(stock.name); onStockClick(stock.ticker) },
        onWatchlistToggle = viewModel::onWatchlistToggle,
        onMarketFilter = viewModel::setMarketFilter,
        onRecentClick = viewModel::onQueryChange,
        onRecentRemove = viewModel::removeRecent,
        onRecentClear = viewModel::clearRecents,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchContent(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onStockClick: (Stock) -> Unit,
    onWatchlistToggle: (Stock) -> Unit = {},
    onMarketFilter: (Market?) -> Unit = {},
    onRecentClick: (String) -> Unit = {},
    onRecentRemove: (String) -> Unit = {},
    onRecentClear: () -> Unit = {},
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Column(Modifier.fillMaxSize().background(c.bg)) {
        SearchBar(query = state.query, onQueryChange = onQueryChange)
        SageSegment(
            options = listOf("전체", "한국", "미국"),
            selectedIndex = when (state.marketFilter) { null -> 0; Market.KR -> 1; Market.US -> 2 },
            onSelect = { onMarketFilter(when (it) { 1 -> Market.KR; 2 -> Market.US; else -> null }) },
            modifier = Modifier.padding(horizontal = dims.screenPadding).padding(bottom = 10.dp),
        )
        HorizontalDivider(color = c.line)

        when (val r = state.displayResults) {
            is Result.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.brand)
            }
            is Result.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(r.message, color = c.textSecondary, style = SageTypography.bodyMedium)
            }
            is Result.Success -> when {
                state.query.isBlank() -> RecentSection(state.recentSearches, onRecentClick, onRecentRemove, onRecentClear)
                r.data.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("검색 결과가 없습니다.", color = c.textSecondary, style = SageTypography.bodyMedium)
                }
                else -> LazyColumn {
                    item { ResultHeader(count = r.data.size) }
                    items(r.data, key = { it.ticker }) { stock ->
                        StockRow(
                            stock = stock,
                            isWatched = stock.ticker in state.watchedTickers,
                            onClick = { onStockClick(stock) },
                            onToggleWatch = { onWatchlistToggle(stock) },
                        )
                        HorizontalDivider(color = c.line)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHeader(count: Int) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dims.screenPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("검색 결과", style = SageTypography.titleSmall, color = c.textPrimary, modifier = Modifier.weight(1f))
        Text("${count}건", style = SageTypography.labelSmall, color = c.textTertiary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSection(
    recents: List<String>,
    onRecentClick: (String) -> Unit,
    onRecentRemove: (String) -> Unit,
    onRecentClear: () -> Unit,
) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    if (recents.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("최근 검색 기록이 없습니다.", color = c.textTertiary, style = SageTypography.bodyMedium)
        }
        return
    }
    Column(Modifier.padding(horizontal = dims.screenPadding, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("최근 검색", style = SageTypography.titleSmall, color = c.textPrimary, modifier = Modifier.weight(1f))
            Text("전체 삭제", style = SageTypography.labelSmall, color = c.textTertiary, modifier = Modifier.clickable(onClick = onRecentClear))
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recents.forEach { term ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.surface)
                        .clickable { onRecentClick(term) }
                        .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(term, style = SageTypography.labelMedium, color = c.textSecondary)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "삭제",
                        tint = c.textTertiary,
                        modifier = Modifier.size(14.dp).clickable { onRecentRemove(term) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    val c = SageTheme.colors
    val dims = SageTheme.dims
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.screenPadding, vertical = 12.dp)
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
            },
        )
    }
}

@Composable
private fun StockRow(stock: Stock, isWatched: Boolean, onClick: () -> Unit, onToggleWatch: () -> Unit) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stock.name, style = SageTypography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(6.dp))
                MiniChip("${stock.market.name} · ${stock.exchange}")
            }
            Spacer(Modifier.height(2.dp))
            Text(stock.ticker, style = SageTypography.labelSmall, color = c.textTertiary)
        }
        IconButton(onClick = onToggleWatch) {
            Icon(
                imageVector = if (isWatched) Icons.Default.Check else Icons.Default.Add,
                contentDescription = if (isWatched) "관심목록에서 제거" else "관심목록에 추가",
                tint = if (isWatched) c.brand else c.textTertiary,
            )
        }
    }
}

@Preview(showBackground = true, name = "검색 — 결과")
@Composable
private fun PreviewResults() = SageStockTheme {
    SearchContent(
        state = SearchUiState(
            query = "삼성",
            results = Result.Success(
                listOf(
                    Stock("005930", "삼성전자", Market.KR, "KOSPI"),
                    Stock("006400", "삼성SDI", Market.KR, "KOSPI"),
                ),
            ),
            watchedTickers = setOf("005930"),
        ),
        onQueryChange = {},
        onStockClick = {},
    )
}

@Preview(showBackground = true, name = "검색 — 최근검색")
@Composable
private fun PreviewRecent() = SageStockTheme {
    SearchContent(
        state = SearchUiState(query = "", recentSearches = listOf("삼성전자", "AAPL", "TSLA")),
        onQueryChange = {},
        onStockClick = {},
    )
}
