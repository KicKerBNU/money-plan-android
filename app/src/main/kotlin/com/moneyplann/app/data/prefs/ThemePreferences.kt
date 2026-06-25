package com.moneyplann.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.themeDataStore by preferencesDataStore("money_plan_theme")

enum class ThemePreference { SYSTEM, LIGHT, DARK }

class ThemePreferences(context: Context) {
    private val dataStore = context.themeDataStore
    private val themeKey = stringPreferencesKey("money-plan-theme")

    private val _preference = MutableStateFlow(ThemePreference.SYSTEM)
    val preference: StateFlow<ThemePreference> = _preference.asStateFlow()

    val useDarkTheme: Boolean?
        get() = when (_preference.value) {
            ThemePreference.SYSTEM -> null
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
        }

    init {
        runBlocking {
            val raw = dataStore.data.first()[themeKey]
            _preference.value = raw?.let { runCatching { ThemePreference.valueOf(it.uppercase()) }.getOrNull() }
                ?: ThemePreference.SYSTEM
        }
    }

    suspend fun setPreference(preference: ThemePreference) {
        _preference.value = preference
        dataStore.edit { it[themeKey] = preference.name.lowercase() }
    }
}
