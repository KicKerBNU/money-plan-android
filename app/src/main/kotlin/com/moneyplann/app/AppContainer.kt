package com.moneyplann.app

import android.content.Context
import com.moneyplann.app.data.api.ApiClient
import com.moneyplann.app.data.api.FinanceApi
import com.moneyplann.app.data.auth.AuthRepository
import com.moneyplann.app.data.chat.ExpenseChatConsentStore
import com.moneyplann.app.data.prefs.MoneyPreferences
import com.moneyplann.app.data.prefs.NotificationPreferences
import com.moneyplann.app.data.prefs.ThemePreferences

object AppContainer {
    lateinit var appContext: Context
        private set

    lateinit var authRepository: AuthRepository
        private set
    lateinit var apiClient: ApiClient
        private set
    lateinit var financeApi: FinanceApi
        private set
    lateinit var moneyPreferences: MoneyPreferences
        private set
    lateinit var themePreferences: ThemePreferences
        private set
    lateinit var notificationPreferences: NotificationPreferences
        private set
    lateinit var expenseChatConsentStore: ExpenseChatConsentStore
        private set

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        authRepository = AuthRepository()
        apiClient = ApiClient(authRepository)
        financeApi = FinanceApi(apiClient)
        moneyPreferences = MoneyPreferences(appContext)
        themePreferences = ThemePreferences(appContext)
        notificationPreferences = NotificationPreferences(appContext)
        expenseChatConsentStore = ExpenseChatConsentStore(appContext)
    }
}
