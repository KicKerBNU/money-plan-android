package com.moneyplann.app.data.chat

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.consentDataStore by preferencesDataStore("money_plan_consent")

class ExpenseChatConsentStore(private val context: Context) {
    private fun key(uid: String) = booleanPreferencesKey("money-plan-expense-chat-ai-consent-$uid")

    fun hasConsent(uid: String): Boolean = runBlocking {
        context.consentDataStore.data.first()[key(uid)] == true
    }

    suspend fun grantConsent(uid: String) {
        context.consentDataStore.edit { it[key(uid)] = true }
    }

    suspend fun revokeConsent(uid: String) {
        context.consentDataStore.edit { it.remove(key(uid)) }
    }
}
