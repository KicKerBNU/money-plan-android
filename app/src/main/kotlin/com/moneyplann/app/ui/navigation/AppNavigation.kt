package com.moneyplann.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.data.models.Category
import com.moneyplann.app.ui.accounts.AccountsScreen
import com.moneyplann.app.ui.chat.ChatbotScreen
import com.moneyplann.app.ui.expenses.ExpenseFormSheet
import com.moneyplann.app.ui.expenses.ExpensesScreen
import com.moneyplann.app.ui.income.IncomeScreen
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
    val scope = rememberCoroutineScope()
    val api = AppContainer.financeApi

    fun openAddExpense() {
        scope.launch {
            try {
                addAccounts = api.fetchAccounts()
                addCategories = api.fetchCategories()
                showAddExpense = true
            } catch (_: Exception) {
            }
        }
    }

    if (showAddExpense) {
        ExpenseFormSheet(
            accounts = addAccounts,
            categories = addCategories,
            onDismiss = { showAddExpense = false },
            onSave = { date, amount, categoryId, accountId, note ->
                scope.launch {
                    try {
                        api.createExpense(date, amount, categoryId, accountId, note)
                        expensesReloadKey++
                        showAddExpense = false
                    } catch (_: Exception) {
                    }
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            if (tab == AppTab.ADD) {
                                openAddExpense()
                                selectedTab = previousTab
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
                                    AppTab.CHAT -> Icons.Default.Chat
                                    AppTab.ACCOUNTS -> Icons.Default.AccountBalance
                                },
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
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
