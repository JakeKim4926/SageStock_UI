package com.sagestock.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Dimens(
    val screenPadding: Dp = 16.dp,
    val sectionPadding: Dp = 16.dp,
    val cardPadding: Dp = 14.dp,
    val gap: Dp = 10.dp,
    val statusBarH: Dp = 44.dp,
    val bottomNavH: Dp = 64.dp,
)

val LocalDimens = staticCompositionLocalOf { Dimens() }
