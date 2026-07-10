package com.moneyplann.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Categories screen tokens. */
object CategoriesTheme {
    private object Light {
        val screen = Color(0xFFFFFFFF)
        val formSheet = Color(0xFFFFFFFF)
        val newTileBorder = Color(0xFFD1D5DB)
        val newTileText = Color(0xFF9CA3AF)
        val iconPickerSelected = Color(0xFF2E7DFF)
        val deleteBadge = Color(0xFF6B7280)
    }

    private object Dark {
        val screen = Color(0xFF121214)
        val formSheet = Color(0xFF1C1C1E)
        val newTileBorder = Color(0xFF6B7280)
        val newTileText = Color(0xFF9CA3AF)
        val iconPickerSelected = Color(0xFF2E7DFF)
        val deleteBadge = Color(0xFF9CA3AF)
    }

    val isDark: Boolean
        @Composable get() = LocalAppDarkTheme.current

    val ScreenBackground: Color
        @Composable get() = if (isDark) Dark.screen else Light.screen

    val FormSheetBackground: Color
        @Composable get() = if (isDark) Dark.formSheet else Light.formSheet

    val NewTileBorder: Color
        @Composable get() = if (isDark) Dark.newTileBorder else Light.newTileBorder

    val NewTileText: Color
        @Composable get() = if (isDark) Dark.newTileText else Light.newTileText

    val IconPickerSelected: Color
        @Composable get() = if (isDark) Dark.iconPickerSelected else Light.iconPickerSelected

    val DeleteBadge: Color
        @Composable get() = if (isDark) Dark.deleteBadge else Light.deleteBadge
}
