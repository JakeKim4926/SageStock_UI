package com.sagestock.ui.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sagestock.ui.components.EmptyState
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

/** Phase 1 스텁 — 와이어프레임 08·08b(가상매매 입력·포트폴리오) 상세는 Phase 5. */
@Composable
fun PaperScreen() {
    val c = SageTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("가상매매", style = SageTypography.titleMedium, color = c.textPrimary)
            Text("Paper Trading 포트폴리오", style = SageTypography.labelSmall, color = c.textTertiary)
        }
        HorizontalDivider(color = c.line)
        EmptyState(
            title = "가상매매는 준비 중입니다",
            desc = "종목 상세에서 가상 매수·매도를 기록하면 여기 포트폴리오에 모여요.",
            icon = Icons.Filled.AccountBalanceWallet,
            modifier = Modifier.weight(1f),
        )
    }
}
