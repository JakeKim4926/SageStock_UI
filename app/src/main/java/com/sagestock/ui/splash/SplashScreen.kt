package com.sagestock.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onAutoLoggedIn: () -> Unit,
    onNeedLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val c = SageTheme.colors
    LaunchedEffect(Unit) {
        delay(600)
        if (viewModel.autoLoggedIn) onAutoLoggedIn() else onNeedLogin()
    }
    Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(c.brand))
            Spacer(Modifier.height(12.dp))
            Text("SageStock", style = SageTypography.headlineSmall, color = c.textPrimary)
        }
    }
}
