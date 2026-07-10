package com.moneyplann.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.notificationDataStore by preferencesDataStore("money_plan_notifications")

class NotificationPreferences(context: Context) {
    private val dataStore = context.notificationDataStore
    private val enabledKey = booleanPreferencesKey("notifications-enabled")

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    init {
        runBlocking {
            _enabled.value = dataStore.data.first()[enabledKey] ?: true
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        dataStore.edit { it[enabledKey] = enabled }
    }
}
