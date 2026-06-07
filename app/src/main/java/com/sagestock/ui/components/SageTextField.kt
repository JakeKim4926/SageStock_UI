package com.sagestock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sagestock.ui.theme.SageTheme
import com.sagestock.ui.theme.SageTypography

private val FieldShape = RoundedCornerShape(12.dp)
private val FieldHeight = 48.dp

/** 라벨 + surface 필드(라운드 12dp). 로그인·검색·가상매매 입력 공용. 에러 시 테두리 up 색. */
@Composable
fun SageTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    isError: Boolean = false,
    isPassword: Boolean = false,
) {
    val c = SageTheme.colors
    Column(modifier) {
        if (label.isNotEmpty()) {
            Text(label, style = SageTypography.labelMedium, color = c.textSecondary)
            Spacer(Modifier.height(6.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FieldHeight)
                .clip(FieldShape)
                .background(c.surface)
                .then(if (isError) Modifier.border(1.dp, c.up, FieldShape) else Modifier)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = c.textTertiary, modifier = Modifier.height(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = SageTypography.bodyMedium.copy(color = c.textPrimary),
                    cursorBrush = SolidColor(c.brand),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = SageTypography.bodyMedium, color = c.textTertiary)
                }
            }
        }
    }
}
