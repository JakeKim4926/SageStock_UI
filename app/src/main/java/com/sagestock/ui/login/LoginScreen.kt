package com.sagestock.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagestock.ui.components.SageButton
import com.sagestock.ui.components.SageTextField
import com.sagestock.ui.theme.SageStockTheme
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn()
    }
    LoginContent(
        state = state,
        onIdChange = viewModel::setId,
        onPasswordChange = viewModel::setPassword,
        onToggleAutoLogin = viewModel::toggleAutoLogin,
        onSubmit = viewModel::login,
    )
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    onIdChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleAutoLogin: () -> Unit,
    onSubmit: () -> Unit,
) {
    val c = SageTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(c.brand))
            Spacer(Modifier.height(12.dp))
            Text("SageStock", style = SageTypography.headlineSmall, color = c.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text("로그인하여 분석을 시작하세요", style = SageTypography.bodySmall, color = c.textSecondary)
            if (state.warmingUp) {
                Spacer(Modifier.height(4.dp))
                Text("서버 깨우는 중…", style = SageTypography.labelMedium, color = c.textTertiary)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            SageTextField(
                label = "아이디",
                value = state.id,
                onValueChange = onIdChange,
                placeholder = "아이디 입력",
                isError = state.error != null,
            )
            SageTextField(
                label = "비밀번호",
                value = state.password,
                onValueChange = onPasswordChange,
                placeholder = "비밀번호 입력",
                isError = state.error != null,
                isPassword = true,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.autoLogin,
                    onCheckedChange = { onToggleAutoLogin() },
                    colors = CheckboxDefaults.colors(checkedColor = c.brand),
                )
                Spacer(Modifier.width(4.dp))
                Text("자동 로그인", style = SageTypography.bodySmall, color = c.textSecondary)
            }

            if (state.error != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.up.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(state.error, style = SageTypography.bodySmall, color = c.up)
                }
            }

            SageButton(text = "로그인", onClick = onSubmit, loading = state.loading)

            Text(
                "비밀번호를 잊으셨나요?  관리자에게 문의",
                style = SageTypography.labelMedium,
                color = c.textTertiary,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Preview(showBackground = true, name = "로그인 — 기본")
@Composable
private fun PreviewLogin() = SageStockTheme {
    LoginContent(LoginUiState(), {}, {}, {}, {})
}

@Preview(showBackground = true, name = "로그인 — 실패")
@Composable
private fun PreviewLoginError() = SageStockTheme {
    LoginContent(
        LoginUiState(id = "admin", password = "x", error = "아이디 또는 비밀번호가 올바르지 않습니다."),
        {}, {}, {}, {},
    )
}
