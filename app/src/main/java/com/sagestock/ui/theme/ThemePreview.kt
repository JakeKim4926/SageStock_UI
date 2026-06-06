package com.sagestock.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Theme — KR 팔레트", showBackground = true)
@Composable
private fun ThemePreviewKR() = SageStockTheme(palette = UpDownPalette.KOREA) {
    PaletteSwatchRow()
}

@Preview(name = "Theme — US 팔레트", showBackground = true)
@Composable
private fun ThemePreviewUS() = SageStockTheme(palette = UpDownPalette.US) {
    PaletteSwatchRow()
}

@Composable
private fun PaletteSwatchRow() {
    val c = SageTheme.colors
    val p = SageTheme.price
    Surface(color = c.bg) {
        Column(Modifier.padding(16.dp)) {
            Text("SageStock Theme", style = SageTypography.titleMedium, color = c.textPrimary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Brand" to c.brand,
                    "Up" to p.up,
                    "Down" to p.down,
                    "Surface" to c.surface,
                ).forEach { (label, color) ->
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Spacer(
                            Modifier
                                .size(40.dp)
                                .background(color, RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(label, style = SageTypography.labelSmall, color = c.textSecondary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("▲ 3.45%", style = PriceTextStyle, color = p.up)
            Text("▼ 1.20%", style = PriceTextStyle, color = p.down)
        }
    }
}
