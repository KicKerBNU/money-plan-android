package com.moneyplann.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneyplann.app.AppContainer
import com.moneyplann.app.core.AnalyticsHelper
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.data.models.Category
import com.moneyplann.app.ui.accounts.AccountsScreen
import com.moneyplann.app.ui.chat.ChatbotScreen
import com.moneyplann.app.ui.expenses.ExpenseFormSheet
import com.moneyplann.app.ui.expenses.ExpensesScreen
import com.moneyplann.app.ui.income.IncomeScreen
import com.moneyplann.app.ui.recurring.RecurringExpensesScreen
import com.moneyplann.app.ui.recurring.RecurringIncomesScreen
import com.moneyplann.app.ui.settings.LocalOpenRecurringExpenses
import com.moneyplann.app.ui.settings.LocalOpenRecurringIncomes
import com.moneyplann.app.ui.settings.LocalOpenSettings
import com.moneyplann.app.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

private enum class AppTab(val label: String) {
    EXPENSES("Expenses"),
    INCOME("Income"),
    ADD("Add"),
    CHAT("Chat"),
    ACCOUNTS("Accounts"),
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(AppTab.EXPENSES.ordinal) }
    var previousTab by remember { mutableIntStateOf(AppTab.EXPENSES.ordinal) }
    var showAddExpense by remember { mutableStateOf(false) }
    var addAccounts by remember { mutableStateOf(emptyList<Account>()) }
    var addCategories by remember { mutableStateOf(emptyList<Category>()) }
    var expensesReloadKey by remember { mutableIntStateOf(0) }
    var showRecurringExpenses by remember { mutableStateOf(false) }
    var showRecurringIncomes by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val api = AppContainer.financeApi
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    LaunchedEffect(selectedTab) {
        when (AppTab.entries[selectedTab]) {
            AppTab.EXPENSES -> AnalyticsHelper.logScreen("expenses")
            AppTab.INCOME -> AnalyticsHelper.logScreen("income")
            AppTab.CHAT -> AnalyticsHelper.logScreen("chatbot")
            AppTab.ACCOUNTS -> AnalyticsHelper.logScreen("accounts")
            AppTab.ADD -> Unit
        }
    }

    fun openAddExpense() {
        showAddExpense = true
        scope.launch {
            try {
                addAccounts = api.fetchAccounts()
                addCategories = api.fetchCategories()
            } catch (_: Exception) {
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalOpenSettings provides { showSettings = true },
            LocalOpenRecurringExpenses provides { showRecurringExpenses = true },
            LocalOpenRecurringIncomes provides { showRecurringIncomes = true },
        ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                ) {
                    AppTab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                if (tab == AppTab.ADD) {
                                    openAddExpense()
                                } else {
                                    previousTab = index
                                    selectedTab = index
                                }
                            },
                            icon = {
                                Icon(
                                    when (tab) {
                                        AppTab.EXPENSES -> Icons.AutoMirrored.Filled.List
                                        AppTab.INCOME -> Icons.Default.Payments
                                        AppTab.ADD -> Icons.Default.Add
                                        AppTab.CHAT -> Icons.AutoMirrored.Filled.Chat
                                        AppTab.ACCOUNTS -> Icons.Default.AccountBalance
                                    },
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = navItemColors,
                        )
                    }
                }
            },
        ) { padding ->
            when (AppTab.entries[selectedTab]) {
                AppTab.EXPENSES -> ExpensesScreen(
                    onAddExpense = ::openAddExpense,
                    reloadKey = expensesReloadKey,
                    modifier = Modifier.padding(padding),
                )
                AppTab.INCOME -> IncomeScreen(Modifier.padding(padding))
                AppTab.CHAT -> ChatbotScreen(Modifier.padding(padding))
                AppTab.ACCOUNTS -> AccountsScreen(Modifier.padding(padding))
                AppTab.ADD -> Unit
            }
        }
        }

        if (showAddExpense) {
            ExpenseFormSheet(
                modifier = Modifier.fillMaxSize(),
                accounts = addAccounts,
                categories = addCategories,
                onDismiss = { showAddExpense = false },
                onSave = { date, amount, categoryId, accountId, note, recurrence ->
                    scope.launch {
                        try {
                            val frequency = if (recurrence?.isCreate == true && recurrence.enabled) {
                                recurrence.frequency
                            } else {
                                null
                            }
                            api.createExpense(date, amount, categoryId, accountId, note, frequency)
                            expensesReloadKey++
                            showAddExpense = false
                        } catch (_: Exception) {
                        }
                    }
                },
            )
        }

        if (showSettings) {
            SettingsScreen(
                modifier = Modifier.fillMaxSize(),
                onDismiss = { showSettings = false },
                onOpenRecurringExpenses = {
                    showSettings = false
                    showRecurringExpenses = true
                },
                onOpenRecurringIncomes = {
                    showSettings = false
                    showRecurringIncomes = true
                },
            )
        }

        if (showRecurringExpenses) {
            RecurringExpensesScreen(
                modifier = Modifier.fillMaxSize(),
                onDismiss = { showRecurringExpenses = false },
            )
        }

        if (showRecurringIncomes) {
            RecurringIncomesScreen(
                modifier = Modifier.fillMaxSize(),
                onDismiss = { showRecurringIncomes = false },
            )
        }
    }
}
