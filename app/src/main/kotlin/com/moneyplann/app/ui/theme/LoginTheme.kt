package com.moneyplann.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Login screen tokens — aligned with the design mockups. */
object LoginTheme {
    private object Light {
        val screen = Color(0xFFF5F5F7)
        val tabBorder = Color(0xFFD1D5DB)
        val tabSelected = Color(0xFFE8E3FA)
        val primaryButton = Color(0xFF5B9FED)
        val primaryButtonText = Color.White
        val googleButtonFill = Color(0xFFFFFFFF)
        val googleButtonStroke = Color(0xFF747775)
        val googleButtonText = Color(0xFF1F1F1F)
        val divider = Color(0xFFE5E7EB)
    }

    private object Dark {
        val screen = Color(0xFF121214)
        val tabBorder = Color(0xFF48484A)
        val tabSelected = Color(0xFF2C2C2E)
        val primaryButton = Color(0xFF1E3A5F)
        val primaryButtonText = Color(0xFF93C5FD)
        val googleButtonFill = Color(0xFF131314)
        val googleButtonStroke = Color(0xFF8E918F)
        val googleButtonText = Color(0xFFE3E3E3)
        val divider = Color(0xFF3A3A3C)
    }

    val isDark: Boolean
        @Composable get() = LocalAppDarkTheme.current

    val ScreenBackground: Color
        @Composable get() = if (isDark) Dark.screen else Light.screen

    val TabBorder: Color
        @Composable get() = if (isDark) Dark.tabBorder else Light.tabBorder

    val TabSelected: Color
        @Composable get() = if (isDark) Dark.tabSelected else Light.tabSelected

    val PrimaryButton: Color
        @Composable get() = if (isDark) Dark.primaryButton else Light.primaryButton

    val PrimaryButtonText: Color
        @Composable get() = if (isDark) Dark.primaryButtonText else Light.primaryButtonText

    val GoogleButtonFill: Color
        @Composable get() = if (isDark) Dark.googleButtonFill else Light.googleButtonFill

    val GoogleButtonStroke: Color
        @Composable get() = if (isDark) Dark.googleButtonStroke else Light.googleButtonStroke

    val GoogleButtonText: Color
        @Composable get() = if (isDark) Dark.googleButtonText else Light.googleButtonText

    val Divider: Color
        @Composable get() = if (isDark) Dark.divider else Light.divider

    val PrimaryText: Color
        @Composable get() = if (isDark) Color(0xFFE5E7EB) else Color(0xFF111827)

    val SecondaryText: Color
        @Composable get() = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
}
