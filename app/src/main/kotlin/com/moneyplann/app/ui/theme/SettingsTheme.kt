package com.moneyplann.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Settings screen tokens. */
object SettingsTheme {
    private object Light {
        val screen = Color(0xFFF5F5F5)
        val group = Color(0xFFFFFFFF)
        val groupBorder = Color(0x14000000)
        val groupElevation = 2.dp
        val iconTint = Color(0xFF374151)
    }

    private object Dark {
        val screen = Color(0xFF121214)
        val group = Color(0xFF1E1E22)
        val groupBorder = Color(0x14FFFFFF)
        val groupElevation = 0.dp
        val iconTint = Color(0xFFE5E7EB)
    }

    val isDark: Boolean
        @Composable get() = LocalAppDarkTheme.current

    val ScreenBackground: Color
        @Composable get() = if (isDark) Dark.screen else Light.screen

    val GroupBackground: Color
        @Composable get() = if (isDark) Dark.group else Light.group

    val GroupBorderColor: Color
        @Composable get() = if (isDark) Dark.groupBorder else Light.groupBorder

    val GroupElevation: Dp
        @Composable get() = if (isDark) Dark.groupElevation else Light.groupElevation

    val IconTint: Color
        @Composable get() = if (isDark) Dark.iconTint else Light.iconTint
}
