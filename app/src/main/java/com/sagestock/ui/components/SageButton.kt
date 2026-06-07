package com.sagestock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

enum class ButtonTone { BRAND, UP, DOWN, OUTLINE }

private val ButtonShape = RoundedCornerShape(12.dp)
private val ButtonHeight = 50.dp

/** 주요 버튼. BRAND=graphite, UP=매수 빨강, DOWN=매도 파랑, OUTLINE=흰 배경+line-2 테두리. */
@Composable
fun SageButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ButtonTone = ButtonTone.BRAND,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val c = SageTheme.colors
    val fill: Color = when (tone) {
        ButtonTone.BRAND -> c.brand
        ButtonTone.UP -> c.up
        ButtonTone.DOWN -> c.down
        ButtonTone.OUTLINE -> c.bg
    }
    val content: Color = if (tone == ButtonTone.OUTLINE) c.brand else c.onBrand
    val clickable = enabled && !loading
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight)
            .clip(ButtonShape)
            .background(if (clickable) fill else fill.copy(alpha = 0.5f))
            .then(if (tone == ButtonTone.OUTLINE) Modifier.border(1.dp, c.line2, ButtonShape) else Modifier)
            .clickable(enabled = clickable, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = content, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(text, style = SageTypography.titleSmall, color = content)
        }
    }
}
