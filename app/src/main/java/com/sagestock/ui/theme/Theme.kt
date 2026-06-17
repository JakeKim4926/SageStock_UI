package com.sagestock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme

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
        ) {
            // Vico 기본 테마는 시스템 다크모드를 따라 흰 글자가 되어 항상 라이트인 우리 배경에서 라벨이 사라진다.
            // 앱 MaterialTheme(라이트)에서 차트 색을 가져오도록 명시해 라벨/축선을 항상 보이게 한다.
            ProvideVicoTheme(rememberM3VicoTheme()) {
                content()
            }
        }
    }
}
