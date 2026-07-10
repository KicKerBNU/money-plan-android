package com.moneyplann.app.ui.settings

import androidx.compose.runtime.staticCompositionLocalOf

/** Opens the full-screen settings overlay from any tab. */
val LocalOpenSettings = staticCompositionLocalOf { {} }

/** Opens the full-screen recurring expenses overlay from Settings on any tab. */
val LocalOpenRecurringExpenses = staticCompositionLocalOf { {} }

/** Opens the full-screen recurring income overlay from Settings on any tab. */
val LocalOpenRecurringIncomes = staticCompositionLocalOf { {} }
