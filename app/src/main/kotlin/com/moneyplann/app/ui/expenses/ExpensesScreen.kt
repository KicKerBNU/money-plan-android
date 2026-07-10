package com.moneyplann.app.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Expense
import com.moneyplann.app.ui.components.CategoryIconView
import com.moneyplann.app.ui.components.EmptyStateView
import com.moneyplann.app.ui.components.ErrorStateView
import com.moneyplann.app.ui.components.FinanceCard
import com.moneyplann.app.ui.components.LoadingStateView
import com.moneyplann.app.ui.components.ScreenHeader
import com.moneyplann.app.ui.settings.SettingsMenu
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.ExpenseTheme
import com.moneyplann.app.util.CurrencyFormatter
import com.moneyplann.app.util.DateUtils
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    val screenBackground = ExpenseTheme.ScreenBackground

    androidx.compose.runtime.LaunchedEffect(reloadKey) { viewModel.load() }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete expense?") },
            text = { Text("This expense will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExpense(expenseToDelete!!)
                    if (editingExpense?.id == expenseToDelete?.id) {
                        editingExpense = null
                    }
                    expenseToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) { Text("Cancel") }
            },
        )
    }

    Box(modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = screenBackground,
            topBar = {
                ScreenHeader(
                    title = "Expenses",
                    backgroundColor = screenBackground,
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
                else -> PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { viewModel.load() },
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item { PeriodPicker(state.periodLabel, onPrev = { viewModel.shiftPeriod(-1) }, onNext = { viewModel.shiftPeriod(1) }) }
                        item {
                            SummaryCard(
                                totalSpent = state.totalSpent,
                                cashFlow = state.cashFlow,
                                breakdown = state.categoryBreakdown,
                                currency = currency,
                            )
                        }
                        if (state.filteredExpenses.isEmpty()) {
                            item {
                                EmptyStateView(
                                    title = "No expenses this month",
                                    message = "Add your first expense for this period.",
                                    actionTitle = "Add expense",
                                    onAction = onAddExpense,
                                )
                            }
                        } else {
                            items(state.dateGroups, key = { it.date }) { group ->
                                DateGroupSection(
                                    group = group,
                                    breakdown = state.categoryBreakdown,
                                    currency = currency,
                                    onEdit = { editingExpense = it },
                                    onDelete = { expenseToDelete = it },
                                )
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
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
                onDelete = {
                    expenseToDelete = editingExpense
                },
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
private fun PeriodPicker(periodLabel: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(periodLabel, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onNext) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SummaryCard(
    totalSpent: Double,
    cashFlow: Double,
    breakdown: List<Pair<String, Double>>,
    currency: String,
) {
    FinanceCard(containerColor = ExpenseTheme.SummaryCardBackground) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Total spent", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
                    Text(
                        CurrencyFormatter.format(totalSpent, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Cash flow", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
                    Text(
                        CurrencyFormatter.formatSigned(cashFlow, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (cashFlow >= 0) ExpenseTheme.CashFlowPositive else AppColors.Danger,
                    )
                }
            }

            if (breakdown.isNotEmpty() && totalSpent > 0) {
                CategoryBar(breakdown, totalSpent)
                breakdown.chunked(2).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { (name, amount) ->
                            val index = breakdown.indexOfFirst { it.first == name && it.second == amount }
                            LegendItem(
                                name = name,
                                amount = amount,
                                color = ExpenseTheme.chartColor(index),
                                currency = currency,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    name: String,
    amount: Double,
    color: Color,
    currency: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(name, style = MaterialTheme.typography.labelSmall, color = AppColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(
            CurrencyFormatter.format(amount, currency),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CategoryBar(breakdown: List<Pair<String, Double>>, totalSpent: Double) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50)),
    ) {
        breakdown.forEachIndexed { index, (_, amount) ->
            Box(
                Modifier
                    .weight((amount / totalSpent).toFloat())
                    .height(10.dp)
                    .background(ExpenseTheme.chartColor(index)),
            )
        }
    }
}

@Composable
private fun DateGroupSection(
    group: ExpenseDateGroup,
    breakdown: List<Pair<String, Double>>,
    currency: String,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                dayHeader(group.date),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Muted,
                letterSpacing = 0.6.sp,
            )
            Text(
                CurrencyFormatter.format(group.total, currency),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Muted,
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = ExpenseTheme.ListElevation,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(ExpenseTheme.ListGroupBackground)
                .border(1.dp, ExpenseTheme.ListBorderColor, RoundedCornerShape(16.dp)),
        ) {
            group.items.forEachIndexed { index, expense ->
                ExpenseRowContent(
                    expense = expense,
                    accentColor = ExpenseTheme.chartColorForCategory(breakdown, expense.categoryName),
                    currency = currency,
                    onEdit = { onEdit(expense) },
                    onDelete = { onDelete(expense) },
                )
                if (index < group.items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 62.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseRowContent(
    expense: Expense,
    accentColor: Color,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier
                .weight(1f)
                .clickable(onClick = onEdit),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryIconView(
                categoryName = expense.categoryName,
                accentColor = accentColor,
                chipBaseSurface = ExpenseTheme.ListGroupBackground,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(expense.categoryName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(
                    buildString {
                        append(expense.accountName)
                        expense.note?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                CurrencyFormatter.format(expense.amount, currency),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Expense options")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { menuExpanded = false; onEdit() },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuExpanded = false; onDelete() },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                )
            }
        }
    }
}

private fun dayHeader(iso: String): String {
    val date = DateUtils.parseLocalIsoDate(iso) ?: return iso
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(Locale.getDefault())
    return "$month ${date.dayOfMonth}, ${date.year}"
}
