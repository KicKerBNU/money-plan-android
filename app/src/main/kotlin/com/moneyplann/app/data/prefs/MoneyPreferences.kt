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
import java.util.Currency
import java.util.Locale

private val Context.moneyDataStore by preferencesDataStore("money_plan_prefs")

class MoneyPreferences(context: Context) {
    private val dataStore = context.moneyDataStore
    private val currencyKey = stringPreferencesKey("money-plan-currency")

    private val _currency = MutableStateFlow<String?>(null)
    val currency: StateFlow<String?> = _currency.asStateFlow()

    val supportedCurrencies = listOf(
        "USD", "EUR", "BRL", "GBP", "CAD", "AUD", "JPY", "CHF", "CNY", "INR", "MXN",
    )

    val autoCurrency: String
        get() = Currency.getInstance(Locale.getDefault()).currencyCode

    val activeCurrency: String
        get() = _currency.value ?: autoCurrency

    init {
        runBlocking {
            val stored = dataStore.data.first()[currencyKey]
            _currency.value = stored?.uppercase()
        }
    }

    suspend fun setCurrency(code: String?) {
        val normalized = code?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        _currency.value = normalized
        dataStore.edit { prefs ->
            if (normalized == null) prefs.remove(currencyKey) else prefs[currencyKey] = normalized
        }
    }
}
