package com.moneyplann.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Income screen tokens — aligned with the design mockups. */
object IncomeTheme {
    private object Light {
        val screen = Color(0xFFF8F8F8)
        val formSheet = Color(0xFFFFFFFF)
        val totalPill = Color(0xFFEDE9FE)
        val totalPillText = Color(0xFF4338CA)
        val listGroup = Color(0xFFFFFFFF)
        val listBorder = Color(0x14000000)
        val listElevation = 2.dp
        val iconAccent = Color(0xFF6366F1)
    }

    private object Dark {
        val screen = Color(0xFF121214)
        val formSheet = Color(0xFF1C1C1E)
        val totalPill = Color(0xFF3D3566)
        val totalPillText = Color(0xFFC7D2FE)
        val listGroup = Color(0xFF1E1E22)
        val listBorder = Color(0x14FFFFFF)
        val listElevation = 0.dp
        val iconAccent = Color(0xFF818CF8)
    }

    val isDark: Boolean
        @Composable get() = LocalAppDarkTheme.current

    val ScreenBackground: Color
        @Composable get() = if (isDark) Dark.screen else Light.screen

    val FormSheetBackground: Color
        @Composable get() = if (isDark) Dark.formSheet else Light.formSheet

    val TotalPillBackground: Color
        @Composable get() = if (isDark) Dark.totalPill else Light.totalPill

    val TotalPillText: Color
        @Composable get() = if (isDark) Dark.totalPillText else Light.totalPillText

    val ListGroupBackground: Color
        @Composable get() = if (isDark) Dark.listGroup else Light.listGroup

    val ListBorderColor: Color
        @Composable get() = if (isDark) Dark.listBorder else Light.listBorder

    val ListElevation: Dp
        @Composable get() = if (isDark) Dark.listElevation else Light.listElevation

    val IconAccent: Color
        @Composable get() = if (isDark) Dark.iconAccent else Light.iconAccent
}
