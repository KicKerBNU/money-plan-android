package com.moneyplann.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Recurring expenses / income screen tokens — aligned with design mockups. */
object RecurringTheme {
    private object Light {
        val screen = Color(0xFFF8F8F8)
        val formSheet = Color(0xFFFFFFFF)
        val card = Color(0xFFFFFFFF)
        val cardBorder = Color(0x14000000)
        val cardElevation = 2.dp
        val frequencyBadge = Color(0xFFEDE9FE)
        val frequencyBadgeText = Color(0xFF4338CA)
    }

    private object Dark {
        val screen = Color(0xFF121214)
        val formSheet = Color(0xFF1C1C1E)
        val card = Color(0xFF1E1E22)
        val cardBorder = Color(0x14FFFFFF)
        val cardElevation = 0.dp
        val frequencyBadge = Color(0xFF3D3566)
        val frequencyBadgeText = Color(0xFFC7D2FE)
    }

    val isDark: Boolean
        @Composable get() = LocalAppDarkTheme.current

    val ScreenBackground: Color
        @Composable get() = if (isDark) Dark.screen else Light.screen

    val FormSheetBackground: Color
        @Composable get() = if (isDark) Dark.formSheet else Light.formSheet

    val CardBackground: Color
        @Composable get() = if (isDark) Dark.card else Light.card

    val CardBorderColor: Color
        @Composable get() = if (isDark) Dark.cardBorder else Light.cardBorder

    val CardElevation: Dp
        @Composable get() = if (isDark) Dark.cardElevation else Light.cardElevation

    val FrequencyBadgeBackground: Color
        @Composable get() = if (isDark) Dark.frequencyBadge else Light.frequencyBadge

    val FrequencyBadgeText: Color
        @Composable get() = if (isDark) Dark.frequencyBadgeText else Light.frequencyBadgeText
}
