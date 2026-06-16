package com.sagestock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sagestock.domain.Signal
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

/** 홈 '오늘의 시그널' 가로 카드(타입칩 + 종목명 + 해석 요약). */
@Composable
fun MiniSignalCard(signal: Signal, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = SageTheme.colors
    val (label, tone) = signal.type.labelAndTone()
    Column(
        modifier = modifier
            .width(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            label,
            style = SageTypography.labelSmall,
            color = tone,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(tone.copy(alpha = 0.12f))
                .border(1.dp, tone.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(signal.stockName, style = SageTypography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(signal.description, style = SageTypography.bodySmall, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
