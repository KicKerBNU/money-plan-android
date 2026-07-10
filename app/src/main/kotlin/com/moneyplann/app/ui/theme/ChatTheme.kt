package com.moneyplann.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Chat / expense assistant screen tokens. */
object ChatTheme {
    private object Light {
        val screen = Color(0xFFFFFFFF)
        val userBubble = Color(0xFF2E7DFF)
        val assistantBubble = Color(0xFFF3F4F6)
        val assistantIconBg = Color(0xFFEDE9FE)
        val assistantIconTint = Color(0xFF6366F1)
        val inputBackground = Color(0xFFF3F4F6)
        val inputBorder = Color(0xFFE5E7EB)
        val sendIdle = Color(0xFFD1D5DB)
        val sendReady = Color(0xFF6B7280)
        val promptBorder = Color(0xFFD1D5DB)
    }

    private object Dark {
        val screen = Color(0xFF121214)
        val userBubble = Color(0xFF2E7DFF)
        val assistantBubble = Color(0xFF2C2C2E)
        val assistantIconBg = Color(0xFF3D3566)
        val assistantIconTint = Color(0xFF818CF8)
        val inputBackground = Color(0xFF1C1C1E)
        val inputBorder = Color(0xFF3A3A3C)
        val sendIdle = Color(0xFF48484A)
        val sendReady = Color(0xFFAEAEB2)
        val promptBorder = Color(0xFF48484A)
    }

    val isDark: Boolean
        @Composable get() = LocalAppDarkTheme.current

    val ScreenBackground: Color
        @Composable get() = if (isDark) Dark.screen else Light.screen

    val UserBubbleBackground: Color
        @Composable get() = if (isDark) Dark.userBubble else Light.userBubble

    val AssistantBubbleBackground: Color
        @Composable get() = if (isDark) Dark.assistantBubble else Light.assistantBubble

    val AssistantIconBackground: Color
        @Composable get() = if (isDark) Dark.assistantIconBg else Light.assistantIconBg

    val AssistantIconTint: Color
        @Composable get() = if (isDark) Dark.assistantIconTint else Light.assistantIconTint

    val InputBackground: Color
        @Composable get() = if (isDark) Dark.inputBackground else Light.inputBackground

    val InputBorder: Color
        @Composable get() = if (isDark) Dark.inputBorder else Light.inputBorder

    val SendIdleBackground: Color
        @Composable get() = if (isDark) Dark.sendIdle else Light.sendIdle

    val SendReadyBackground: Color
        @Composable get() = if (isDark) Dark.sendReady else Light.sendReady

    val PromptBorder: Color
        @Composable get() = if (isDark) Dark.promptBorder else Light.promptBorder
}
