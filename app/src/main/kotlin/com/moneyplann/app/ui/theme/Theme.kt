package com.moneyplann.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    val Primary = Color(0xFF2563EB)
    val Muted = Color(0xFF64748B)
    val Positive = Color(0xFF16A34A)
    val Danger = Color(0xFFDC2626)
}

private val LightColors = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.White,
    secondary = AppColors.Muted,
    error = AppColors.Danger,
)

private val DarkColors = darkColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.White,
    secondary = AppColors.Muted,
    error = AppColors.Danger,
)

@Composable
fun MoneyPlanTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val isDark = darkTheme ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        content = content,
    )
}
