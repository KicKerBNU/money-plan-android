package com.moneyplann.app.ui.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.data.models.IncomeEntry
import com.moneyplann.app.data.models.RecurrenceFrequency
import com.moneyplann.app.data.models.RecurrenceSave
import com.moneyplann.app.data.models.RecurringIncome
import com.moneyplann.app.ui.components.ErrorStateView
import com.moneyplann.app.ui.components.FinanceCard
import com.moneyplann.app.ui.components.KpiView
import com.moneyplann.app.ui.components.LoadingStateView
import com.moneyplann.app.ui.settings.SettingsMenu
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.util.CurrencyFormatter
import com.moneyplann.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class IncomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val entries: List<IncomeEntry> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val recurringIncomes: List<RecurringIncome> = emptyList(),
    val year: Int = DateUtils.currentYearMonth().first,
    val month: Int = DateUtils.currentYearMonth().second,
) {
    val total: Double get() = entries.sumOf { it.amount }
    val lastDate: String? get() = entries.maxByOrNull { it.date }?.date

    val periodLabel: String
        get() = YearMonth.of(year, month)
            .month
            .getDisplayName(TextStyle.FULL, Locale.getDefault()) + " $year"
}

class IncomeViewModel : ViewModel() {
    private val api = AppContainer.financeApi
    private val _state = MutableStateFlow(IncomeUiState())
    val state: StateFlow<IncomeUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val year = _state.value.year
                val month = _state.value.month
                val entries = api.fetchIncomes(year, month)
                val accounts = api.fetchAccounts()
                val recurring = runCatching { api.fetchRecurringIncomes() }.getOrDefault(emptyList())
                _state.update {
                    it.copy(
                        isLoading = false,
                        entries = entries,
                        accounts = accounts,
                        recurringIncomes = recurring,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun shiftPeriod(delta: Int) {
        val current = YearMonth.of(_state.value.year, _state.value.month).plusMonths(delta.toLong())
        _state.update { it.copy(year = current.year, month = current.monthValue) }
        load()
    }

    fun linkedRecurring(entry: IncomeEntry): RecurringIncome? {
        val recurringId = entry.recurringIncomeId ?: return null
        return _state.value.recurringIncomes.firstOrNull { it.id == recurringId }
    }

    fun create(
        date: String,
        amount: Double,
        accountId: Int,
        note: String?,
        recurrence: RecurrenceSave?,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                val frequency = if (recurrence?.isCreate == true && recurrence.enabled) recurrence.frequency else null
                api.createIncome(date, amount, accountId, note, frequency)
                load()
                onSuccess()
            } catch (_: Exception) {
            }
        }
    }

    fun saveEdit(
        original: IncomeEntry,
        date: String,
        amount: Double,
        accountId: Int,
        note: String?,
        recurrence: RecurrenceSave?,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val (recurrenceEnabled, recurrenceFrequency) = recurrencePayload(recurrence, original)
            try {
                api.updateIncome(
                    id = original.id,
                    date = date,
                    amount = amount,
                    accountId = accountId,
                    note = note,
                    recurrenceEnabled = recurrenceEnabled,
                    recurrenceFrequency = recurrenceFrequency,
                )
                load()
                onSuccess()
            } catch (_: Exception) {
            }
        }
    }

    private fun recurrencePayload(
        recurrence: RecurrenceSave?,
        original: IncomeEntry,
    ): Pair<Boolean?, RecurrenceFrequency?> {
        if (recurrence == null || recurrence.isCreate) return null to null
        if (original.recurringIncomeId == null && !recurrence.enabled) return null to null
        return recurrence.enabled to if (recurrence.enabled) recurrence.frequency else null
    }

    fun delete(entry: IncomeEntry) {
        viewModelScope.launch {
            val snapshot = _state.value.entries
            _state.update { it.copy(entries = it.entries.filterNot { e -> e.id == entry.id }) }
            try {
                api.deleteIncome(entry.id)
            } catch (_: Exception) {
                _state.update { it.copy(entries = snapshot) }
                load()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(modifier: Modifier = Modifier, viewModel: IncomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val currency = AppContainer.moneyPreferences.activeCurrency
    var showAddSheet by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<IncomeEntry?>(null) }
    var toDelete by remember { mutableStateOf<IncomeEntry?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete income?") },
            text = { Text("This income entry will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(toDelete!!)
                    toDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }

    Box(modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Income") },
                    actions = { SettingsMenu() },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add income")
                }
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
                        Text("Salary, freelance, rent, and other money in.", color = AppColors.Muted)
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { viewModel.shiftPeriod(-1) }) {
                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
                            }
                            Text(state.periodLabel, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { viewModel.shiftPeriod(1) }) {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
                            }
                        }
                    }
                    item {
                        FinanceCard {
                            KpiView("Total income", CurrencyFormatter.format(state.total, currency))
                            state.lastDate?.let {
                                Text(
                                    "${state.entries.size} entries · last on ${DateUtils.formatShortDate(it)}",
                                    color = AppColors.Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Button(onClick = { showAddSheet = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Add income")
                            }
                        }
                    }
                    if (state.entries.isEmpty()) {
                        item {
                            FinanceCard {
                                Text("No income this month", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Add salary, freelance payments, rent, or any other money coming in.",
                                    color = AppColors.Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    } else {
                        items(state.entries, key = { it.id }) { entry ->
                            IncomeRow(
                                entry = entry,
                                currency = currency,
                                isRecurring = entry.recurringIncomeId != null,
                                onEdit = { editingEntry = entry },
                                onDelete = { toDelete = entry },
                            )
                        }
                    }
                }
            }
        }

        if (showAddSheet) {
            IncomeFormSheet(
                modifier = Modifier.fillMaxSize(),
                accounts = state.accounts,
                onDismiss = { showAddSheet = false },
                onSave = { date, amount, accountId, note, recurrence ->
                    viewModel.create(date, amount, accountId, note, recurrence) {
                        showAddSheet = false
                    }
                },
            )
        }

        editingEntry?.let { entry ->
            IncomeFormSheet(
                modifier = Modifier.fillMaxSize(),
                accounts = state.accounts,
                editing = entry,
                linkedRecurring = viewModel.linkedRecurring(entry),
                onDismiss = { editingEntry = null },
                onSave = { date, amount, accountId, note, recurrence ->
                    viewModel.saveEdit(entry, date, amount, accountId, note, recurrence) {
                        editingEntry = null
                    }
                },
            )
        }
    }
}

@Composable
private fun IncomeRow(
    entry: IncomeEntry,
    currency: String,
    isRecurring: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    FinanceCard {
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        CurrencyFormatter.format(entry.amount, currency),
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Positive,
                    )
                    if (isRecurring) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Recurring income",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(0.dp),
                        )
                    }
                }
            },
            supportingContent = {
                Column {
                    Text(entry.note?.takeIf { it.isNotBlank() } ?: entry.accountName ?: "Income")
                    Text(
                        "${DateUtils.formatShortDate(entry.date)} · ${entry.accountName.orEmpty()}",
                        color = AppColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            trailingContent = {
                IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { expanded = false; onEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { expanded = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                    )
                }
            },
        )
    }
}
