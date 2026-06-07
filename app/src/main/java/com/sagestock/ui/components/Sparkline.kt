package com.sagestock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sagestock.ui.theme.SageTheme

/** 미니 추세선. 등락 부호에 따라 up/down 색(접근성 보조 — 행 텍스트의 ▲▼와 병행). */
@Composable
fun Sparkline(data: List<Double>, up: Boolean, modifier: Modifier = Modifier) {
    val color = if (up) SageTheme.price.up else SageTheme.price.down
    Canvas(modifier.size(54.dp, 28.dp)) {
        if (data.size < 2) return@Canvas
        val min = data.minOrNull() ?: return@Canvas
        val max = data.maxOrNull() ?: return@Canvas
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val stepX = size.width / (data.size - 1)
        val path = Path()
        data.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - min) / range * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
    }
}
