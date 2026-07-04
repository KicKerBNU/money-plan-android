package com.moneyplann.app.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Expense
import com.moneyplann.app.ui.components.EmptyStateView
import com.moneyplann.app.ui.components.ErrorStateView
import com.moneyplann.app.ui.components.FinanceCard
import com.moneyplann.app.ui.components.KpiView
import com.moneyplann.app.ui.components.LoadingStateView
import com.moneyplann.app.ui.settings.SettingsMenu
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.util.CurrencyFormatter
import com.moneyplann.app.util.DateUtils
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpensesScreen(
    onAddExpense: () -> Unit,
    reloadKey: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: ExpensesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currency = AppContainer.moneyPreferences.activeCurrency
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var showTripRange by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(reloadKey) { viewModel.load() }

    if (showTripRange) {
        TripRangeSheet(
            onDismiss = { showTripRange = false },
            onApply = { start, end ->
                viewModel.setDateRange(start, end)
                showTripRange = false
            },
        )
    }

    if (expenseToDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete expense?") },
            text = { Text("This expense will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExpense(expenseToDelete!!)
                    expenseToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { expenseToDelete = null }) { Text("Cancel") } },
        )
    }

    Box(modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Expenses") },
                    actions = {
                        IconButton(onClick = onAddExpense) {
                            Icon(Icons.Default.Add, contentDescription = "Add expense")
                        }
                        SettingsMenu()
                    },
                )
            },
        ) { padding ->
            when {
                state.isLoading -> LoadingStateView(Modifier.padding(padding))
                state.errorMessage != null -> ErrorStateView(state.errorMessage!!, Modifier.padding(padding))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        OutlinedTextField(
                            value = state.searchText,
                            onValueChange = viewModel::setSearch,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search expenses") },
                            singleLine = true,
                        )
                    }
                    item {
                        Text("${state.expenses.size} entries this month", color = AppColors.Muted)
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FinanceCard(Modifier.weight(1f)) {
                                KpiView("Total spent", CurrencyFormatter.format(state.totalSpent, currency))
                            }
                            FinanceCard(Modifier.weight(1f)) {
                                KpiView(
                                    "Cash flow",
                                    CurrencyFormatter.formatSigned(state.cashFlow, currency),
                                    if (state.cashFlow >= 0) AppColors.Positive else AppColors.Danger,
                                )
                            }
                        }
                    }
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = { viewModel.applyQuickRange(3) }, label = { Text("Last 3 days") })
                            AssistChip(onClick = { viewModel.applyQuickRange(7) }, label = { Text("Last 7 days") })
                            AssistChip(onClick = { showTripRange = true }, label = { Text("Custom range") })
                            if (state.dateRangeStart != null) {
                                AssistChip(onClick = { viewModel.clearDateRange() }, label = { Text("Clear dates") })
                            }
                        }
                    }
                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.selectedCategoryIds.isEmpty(),
                                onClick = { viewModel.clearCategories() },
                                label = { Text("All") },
                            )
                            state.categories.take(6).forEach { category ->
                                FilterChip(
                                    selected = state.selectedCategoryIds.contains(category.id),
                                    onClick = { viewModel.toggleCategory(category.id) },
                                    label = { Text(category.name) },
                                )
                            }
                        }
                    }
                    if (state.filteredExpenses.isEmpty()) {
                        item {
                            EmptyStateView(
                                title = "No expenses yet",
                                message = "Add your first expense for this month.",
                                actionTitle = "Add expense",
                                onAction = onAddExpense,
                            )
                        }
                    } else {
                        items(state.filteredExpenses, key = { it.id }) { expense ->
                            ExpenseRow(
                                expense = expense,
                                currency = currency,
                                onEdit = { editingExpense = expense },
                                onDelete = { expenseToDelete = expense },
                            )
                        }
                    }
                    if (state.categoryBreakdown.isNotEmpty()) {
                        item {
                            FinanceCard {
                                Text("By category", fontWeight = FontWeight.SemiBold)
                                state.categoryBreakdown.forEach { (name, amount) ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(name)
                                        Text(CurrencyFormatter.format(amount, currency))
                                    }
                                }
                            }
                        }
                    }
                    if (state.accounts.isNotEmpty()) {
                        item {
                            FinanceCard {
                                Text("By account", fontWeight = FontWeight.SemiBold)
                                state.accounts.forEach { account ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(account.name)
                                        Text(CurrencyFormatter.format(account.currentBalance, currency))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (editingExpense != null) {
            ExpenseFormSheet(
                modifier = Modifier.fillMaxSize(),
                accounts = state.accounts,
                categories = state.categories,
                editing = editingExpense,
                linkedRecurring = editingExpense?.let { viewModel.linkedRecurring(it) },
                onDismiss = { editingExpense = null },
                onSave = { date, amount, categoryId, accountId, note, recurrence ->
                    viewModel.saveExpenseEdit(
                        original = editingExpense!!,
                        date = date,
                        amount = amount,
                        categoryId = categoryId,
                        accountId = accountId,
                        note = note,
                        recurrence = recurrence,
                    )
                    editingExpense = null
                },
            )
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    FinanceCard {
        ListItem(
            headlineContent = {
                Text(CurrencyFormatter.format(expense.amount, currency), fontWeight = FontWeight.SemiBold)
            },
            supportingContent = {
                Column {
                    Text("${expense.categoryName} · ${expense.accountName}")
                    Text(DateUtils.formatShortDate(expense.date), color = AppColors.Muted, style = MaterialTheme.typography.bodySmall)
                    expense.note?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            },
            trailingContent = {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { menuExpanded = false; onEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menuExpanded = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripRangeSheet(onDismiss: () -> Unit, onApply: (String, String) -> Unit) {
    val startState = rememberDatePickerState()
    val endState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val start = startState.selectedDateMillis?.let {
                    DateUtils.localIsoDate(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                } ?: return@TextButton
                val end = endState.selectedDateMillis?.let {
                    DateUtils.localIsoDate(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                } ?: return@TextButton
                onApply(start, end)
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        Column {
            Text("From", modifier = Modifier.padding(16.dp))
            DatePicker(state = startState)
            Text("To", modifier = Modifier.padding(16.dp))
            DatePicker(state = endState)
        }
    }
}
