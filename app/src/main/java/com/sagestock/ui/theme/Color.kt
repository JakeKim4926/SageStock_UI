package com.sagestock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Raw 토큰 ────────────────────────────────────────────
val Bg          = Color(0xFFFFFFFF)
val Surface     = Color(0xFFF4F5F7)
val Surface2    = Color(0xFFECEEF1)
val Line        = Color(0xFFE4E7EC)
val Line2       = Color(0xFFCDD2DA)
val Brand       = Color(0xFF2B2F36)
val OnBrand     = Color(0xFFFFFFFF)

val Up          = Color(0xFFE0392B)
val Down        = Color(0xFF1565C0)
val Positive    = Color(0xFF2E9E5B)
val Warning     = Color(0xFFC77700)

val TextPrimary   = Color(0xFF1B1E23)
val TextSecondary = Color(0xFF6A7280)
val TextTertiary  = Color(0xFF9AA1AC)

// ── 커스텀 컬러 홀더 ────────────────────────────────────
@Immutable
data class SageStockColors(
    val bg: Color, val surface: Color, val surface2: Color,
    val line: Color, val line2: Color, val brand: Color, val onBrand: Color,
    val up: Color, val down: Color, val positive: Color, val warning: Color,
    val textPrimary: Color, val textSecondary: Color, val textTertiary: Color,
)

val LightColors = SageStockColors(
    bg = Bg, surface = Surface, surface2 = Surface2, line = Line, line2 = Line2, brand = Brand, onBrand = OnBrand,
    up = Up, down = Down, positive = Positive, warning = Warning,
    textPrimary = TextPrimary, textSecondary = TextSecondary, textTertiary = TextTertiary,
)

val LocalSageStockColors = staticCompositionLocalOf { LightColors }

// ── 상승/하락 색상 (KR ↔ US 전환) ───────────────────────
enum class UpDownPalette { KOREA, US }

@Immutable
data class PriceColors(val up: Color, val down: Color, val flat: Color)

fun priceColorsFor(palette: UpDownPalette) = when (palette) {
    UpDownPalette.KOREA -> PriceColors(up = Up,       down = Down, flat = TextTertiary)
    UpDownPalette.US    -> PriceColors(up = Positive, down = Up,   flat = TextTertiary)
}

val LocalPriceColors = staticCompositionLocalOf { priceColorsFor(UpDownPalette.KOREA) }

@Composable
fun priceColorOf(change: Double): Color = with(LocalPriceColors.current) {
    when { change > 0 -> up; change < 0 -> down; else -> flat }
}

fun arrowOf(change: Double): String = when { change > 0 -> "▲"; change < 0 -> "▼"; else -> "–" }
