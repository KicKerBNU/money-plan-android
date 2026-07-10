package com.moneyplann.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Mirrors the app's chosen theme (Settings → Light/Dark), not just the OS setting. */
val LocalAppDarkTheme = staticCompositionLocalOf { false }

object AppColors {
    val Primary: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val Muted: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    /** Primary labels on screen backgrounds — dark text in light mode, light text in dark mode. */
    val PrimaryLabel: Color
        @Composable get() = if (LocalAppDarkTheme.current) Color(0xFFE5E7EB) else Color(0xFF111827)

    /** Secondary / muted labels — always lower contrast than [PrimaryLabel]. */
    val SecondaryLabel: Color
        @Composable get() = if (LocalAppDarkTheme.current) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    /** Stronger green on light surfaces; lighter green on dark surfaces (Material contrast). */
    val Positive: Color
        @Composable get() = if (LocalAppDarkTheme.current) Color(0xFF4ADE80) else Color(0xFF15803D)

    val CashFlowPositive: Color
        @Composable get() = Positive

    /** iOS-style blue for form Cancel / Save actions. */
    val ActionBlue: Color
        @Composable get() = if (LocalAppDarkTheme.current) Color(0xFF64B5FF) else Color(0xFF007AFF)

    val Danger: Color
        @Composable get() = MaterialTheme.colorScheme.error

    val Surface: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val SurfaceSoft: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerLow

    @Composable
    fun iconChipBackground(accent: Color, baseSurface: Color = Surface): Color {
        val isDark = LocalAppDarkTheme.current
        val mix = if (isDark) 0.42f else 0.14f
        val base = if (isDark) baseSurface else Color.White
        return lerp(base, accent, mix)
    }

    @Composable
    fun iconChipForeground(accent: Color): Color {
        val mix = if (LocalAppDarkTheme.current) 0.38f else 0f
        return if (mix <= 0f) accent else lerp(accent, Color.White, mix)
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E3FA),
    onPrimaryContainer = Color(0xFF4338CA),
    secondary = Color(0xFF6B7280),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF4338CA),
    tertiary = Color(0xFF15803D),
    onTertiary = Color.White,
    error = Color(0xFFDC2626),
    onError = Color.White,
    background = Color(0xFFF8F8F8),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceContainer = Color(0xFFF3F4F6),
    surfaceContainerLow = Color(0xFFF8F8F8),
    surfaceContainerHigh = Color(0xFFE5E7EB),
    outline = Color(0xFFE5E7EB),
    outlineVariant = Color(0xFFD1D5DB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3D3566),
    onPrimaryContainer = Color(0xFFC7D2FE),
    secondary = Color(0xFF9CA3AF),
    onSecondary = Color(0xFF1F2937),
    secondaryContainer = Color(0xFF3D3566),
    onSecondaryContainer = Color(0xFF818CF8),
    tertiary = Color(0xFF4ADE80),
    onTertiary = Color(0xFF052E16),
    error = Color(0xFFEF6F82),
    onError = Color(0xFF450A0A),
    background = Color(0xFF121214),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF1E1E22),
    onSurface = Color(0xFFE5E7EB),
    onSurfaceVariant = Color(0xFF9CA3AF),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerLow = Color(0xFF121214),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    outline = Color(0xFF2C2C2E),
    outlineVariant = Color(0xFF3F3F46),
)

@Composable
fun MoneyPlanTheme(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val isDark = darkTheme ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        content = {
            CompositionLocalProvider(LocalAppDarkTheme provides isDark) {
                content()
            }
        },
    )
}
