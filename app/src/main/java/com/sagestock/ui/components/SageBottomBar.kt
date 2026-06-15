package com.sagestock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

/** 하단 5탭. route는 NavGraph의 탭 라우트와 1:1. */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "홈", Icons.Filled.Home),
    SEARCH("search", "검색", Icons.Filled.Search),
    SIGNALS("signals", "시그널", Icons.Filled.TrendingUp),
    PAPER("paper", "가상매매", Icons.Filled.AccountBalanceWallet),
    SETTINGS("settings", "설정", Icons.Filled.Settings);

    companion object {
        val routes: Set<String> = entries.map { it.route }.toSet()
    }
}

@Composable
fun SageBottomBar(currentRoute: String?, onSelect: (Tab) -> Unit) {
    val c = SageTheme.colors
    Column(Modifier.background(c.bg)) {
        HorizontalDivider(color = c.line)
        Row(
            // edge-to-edge: 시스템 네비게이션 바 인셋만큼 콘텐츠를 위로 올린다(배경은 인셋 영역까지 덮음).
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                val tint = if (selected) c.brand else c.textTertiary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
                    Text(
                        tab.label,
                        style = SageTypography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                        color = tint,
                    )
                }
            }
        }
    }
}
