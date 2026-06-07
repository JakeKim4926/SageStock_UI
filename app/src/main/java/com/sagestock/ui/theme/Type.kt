package com.sagestock.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sagestock.R

val GmarketSans = FontFamily(
    // Normal(400)은 Gmarket에 대응 웨이트가 없어 Medium 파일로 매핑
    Font(R.font.gmarket_sans_medium, FontWeight.Normal),
    Font(R.font.gmarket_sans_medium, FontWeight.Medium),
    Font(R.font.gmarket_sans_bold, FontWeight.Bold),
)

val SageTypography = Typography(
    headlineSmall = TextStyle(fontFamily = GmarketSans, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp),
    titleMedium   = TextStyle(fontFamily = GmarketSans, fontSize = 16.sp, fontWeight = FontWeight.Bold),
    titleSmall    = TextStyle(fontFamily = GmarketSans, fontSize = 15.sp, fontWeight = FontWeight.Bold),
    bodyMedium    = TextStyle(fontFamily = GmarketSans, fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall     = TextStyle(fontFamily = GmarketSans, fontSize = 13.sp, fontWeight = FontWeight.Medium),
    labelMedium   = TextStyle(fontFamily = GmarketSans, fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall    = TextStyle(fontFamily = GmarketSans, fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

val PriceTextStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Bold,
    fontFamily = FontFamily.Monospace,
)

val PriceLargeTextStyle = TextStyle(
    fontSize = 26.sp,
    fontWeight = FontWeight.Bold,
)
