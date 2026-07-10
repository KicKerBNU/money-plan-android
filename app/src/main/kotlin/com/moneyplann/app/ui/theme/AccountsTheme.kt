package com.moneyplann.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Accounts screen tokens — aligned with the design mockups. */
object AccountsTheme {
    private object Light {
        val screen = Color(0xFFF5F5F5)
        val formSheet = Color(0xFFFFFFFF)
        val listGroup = Color(0xFFFFFFFF)
        val listBorder = Color(0x14000000)
        val listElevation = 2.dp
        val featuredGradient = listOf(Color(0xFFE4E4E8), Color(0xFFF8F8FA), Color(0xFFD8D8DE))
        val featuredIconScrim = Color(0x33FFFFFF)
        val defaultBadgeBg = Color(0xFFEDE9FE)
        val defaultBadgeText = Color(0xFF4338CA)
    }

    private object Dark {
        val screen = Color(0xFF121214)
        val formSheet = Color(0xFF1C1C1E)
        val listGroup = Color(0xFF1E1E22)
        val listBorder = Color(0x14FFFFFF)
        val listElevation = 0.dp
        val featuredGradient = listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E), Color(0xFF3A3A3C))
        val featuredIconScrim = Color(0x33FFFFFF)
        val defaultBadgeBg = Color(0xFF3D3566)
        val defaultBadgeText = Color(0xFFC7D2FE)
    }

    val isDark: Boolean
        @Composable get() = LocalAppDarkTheme.current

    val ScreenBackground: Color
        @Composable get() = if (isDark) Dark.screen else Light.screen

    val FormSheetBackground: Color
        @Composable get() = if (isDark) Dark.formSheet else Light.formSheet

    val ListGroupBackground: Color
        @Composable get() = if (isDark) Dark.listGroup else Light.listGroup

    val ListBorderColor: Color
        @Composable get() = if (isDark) Dark.listBorder else Light.listBorder

    val ListElevation: Dp
        @Composable get() = if (isDark) Dark.listElevation else Light.listElevation

    val FeaturedCardGradient: Brush
        @Composable get() {
            val colors = if (isDark) Dark.featuredGradient else Light.featuredGradient
            return Brush.linearGradient(colors)
        }

    val FeaturedIconScrim: Color
        @Composable get() = if (isDark) Dark.featuredIconScrim else Light.featuredIconScrim

    val DefaultBadgeBackground: Color
        @Composable get() = if (isDark) Dark.defaultBadgeBg else Light.defaultBadgeBg

    val DefaultBadgeText: Color
        @Composable get() = if (isDark) Dark.defaultBadgeText else Light.defaultBadgeText
}
