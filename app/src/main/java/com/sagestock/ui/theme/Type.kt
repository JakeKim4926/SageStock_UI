package com.sagestock.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SageTypography = Typography(
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp),
    titleMedium   = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    titleSmall    = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
    bodyMedium    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall     = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    labelMedium   = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
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
