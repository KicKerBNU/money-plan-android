package com.moneyplann.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Expenses screen surface tokens — chart colors live in [CategoryColors]. */
object ExpenseTheme {
    private object Light {
        val screen = Color(0xFFF8F8F8)
        val summary = Color(0xFFE8E3FA)
        val listGroup = Color(0xFFFFFFFF)
        val listBorder = Color(0x14000000)
        val listElevation = 2.dp
    }

    private object Dark {
        val screen = Color(0xFF121214)
        val summary = Color(0xFF2E1E5E)
        val listGroup = Color(0xFF1E1E22)
        val listBorder = Color(0x14FFFFFF)
        val listElevation = 0.dp
    }

    val isDark: Boolean
        @Composable get() = LocalAppDarkTheme.current

    val ScreenBackground: Color
        @Composable get() = if (isDark) Dark.screen else Light.screen

    val SummaryCardBackground: Color
        @Composable get() = if (isDark) Dark.summary else Light.summary

    val ListGroupBackground: Color
        @Composable get() = if (isDark) Dark.listGroup else Light.listGroup

    val CashFlowPositive: Color
        @Composable get() = AppColors.CashFlowPositive

    val ListBorderColor: Color
        @Composable get() = if (isDark) Dark.listBorder else Light.listBorder

    val ListElevation: Dp
        @Composable get() = if (isDark) Dark.listElevation else Light.listElevation

    fun chartColor(index: Int): Color = CategoryColors.accent(index)

    fun chartColorForCategory(breakdown: List<Pair<String, Double>>, categoryName: String): Color =
        CategoryColors.accentForCategory(categoryName, breakdown)
}
