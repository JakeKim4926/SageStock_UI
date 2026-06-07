package com.sagestock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sagestock.domain.Market
import com.sagestock.domain.MarketStatus
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

/** 작은 시장칩(KR/US). surface-2 배경 pill. */
@Composable
fun MiniChip(text: String, modifier: Modifier = Modifier) {
    val c = SageTheme.colors
    Text(
        text = text,
        style = SageTypography.labelSmall,
        color = c.textTertiary,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.surface2)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** 장상태 배지(점 + 라벨). 장중 positive·프리/애프터 warning·장마감 grey. */
@Composable
fun StatusBadge(label: String, dotColor: Color?, modifier: Modifier = Modifier) {
    val c = SageTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(dotColor))
            Spacer(Modifier.width(5.dp))
        }
        Text(label, style = SageTypography.labelSmall, color = c.textSecondary)
    }
}

/** 시장 + 장상태 → 라벨/색 매핑 배지. */
@Composable
fun MarketStatusBadge(market: Market, status: MarketStatus, modifier: Modifier = Modifier) {
    val c = SageTheme.colors
    val prefix = if (market == Market.KR) "KR" else "US"
    val (text, dot) = when (status) {
        MarketStatus.OPEN -> "$prefix 장중" to c.positive
        MarketStatus.PRE_MARKET -> "$prefix 프리마켓" to c.warning
        MarketStatus.AFTER_MARKET -> "$prefix 애프터마켓" to c.warning
        MarketStatus.CLOSED -> "$prefix 장마감" to c.textTertiary
    }
    StatusBadge(label = text, dotColor = dot, modifier = modifier)
}
