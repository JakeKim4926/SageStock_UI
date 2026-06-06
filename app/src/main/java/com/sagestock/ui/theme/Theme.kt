package com.sagestock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

object SageTheme {
    val colors: SageStockColors @Composable get() = LocalSageStockColors.current
    val dims: Dimens             @Composable get() = LocalDimens.current
    val price: PriceColors       @Composable get() = LocalPriceColors.current
}

@Composable
fun SageStockTheme(
    palette: UpDownPalette = UpDownPalette.KOREA,
    content: @Composable () -> Unit,
) {
    val colors = LightColors
    val m3 = lightColorScheme(
        primary = colors.brand,
        background = colors.bg,
        surface = colors.surface,
        onBackground = colors.textPrimary,
    )
    CompositionLocalProvider(
        LocalSageStockColors provides colors,
        LocalPriceColors provides priceColorsFor(palette),
        LocalDimens provides Dimens(),
    ) {
        MaterialTheme(
            colorScheme = m3,
            typography = SageTypography,
            shapes = SageShapes,
            content = content,
        )
    }
}
