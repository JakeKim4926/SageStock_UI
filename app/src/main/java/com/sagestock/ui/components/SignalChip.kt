package com.sagestock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sagestock.domain.SignalType
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

/** 시그널 타입 → 한글 라벨 + 의미색 톤. 홈 시그널 카드·랭킹 등에서 공용. */
@Composable
fun SignalType.labelAndTone(): Pair<String, Color> = when (this) {
    SignalType.GOLDEN_CROSS -> "골든크로스" to SageTheme.price.up
    SignalType.DEAD_CROSS -> "데드크로스" to SageTheme.price.down
    SignalType.RSI_OVERSOLD -> "RSI 과매도" to SageTheme.colors.positive
    SignalType.RSI_OVERBOUGHT -> "RSI 과매수" to SageTheme.colors.warning
    SignalType.BOLLINGER_BREAKOUT -> "볼린저 돌파" to SageTheme.colors.warning
    SignalType.BULLISH_DIVERGENCE -> "상승 다이버전스" to SageTheme.price.up
    SignalType.BEARISH_DIVERGENCE -> "하락 다이버전스" to SageTheme.price.down
}

/** 시그널 타입 pill 칩(라벨 + 톤 배경/테두리). */
@Composable
fun SignalChip(type: SignalType, modifier: Modifier = Modifier) {
    val (label, tone) = type.labelAndTone()
    Text(
        label,
        style = SageTypography.labelSmall,
        color = tone,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(tone.copy(alpha = 0.12f))
            .border(1.dp, tone.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
