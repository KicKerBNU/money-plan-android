package com.moneyplann.app.ui.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.IncomeEntry
import com.moneyplann.app.ui.components.ErrorStateView
import com.moneyplann.app.ui.components.FinanceCard
import com.moneyplann.app.ui.components.KpiView
import com.moneyplann.app.ui.components.LoadingStateView
import com.moneyplann.app.ui.settings.SettingsMenu
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.util.CurrencyFormatter
import com.moneyplann.app.util.DateUtils
import com.moneyplann.app.util.DefaultAccountPicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class IncomeUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val entries: List<IncomeEntry> = emptyList(),
    val accounts: List<com.moneyplann.app.data.models.Account> = emptyList(),
) {
    val total: Double get() = entries.sumOf { it.amount }
    val lastDate: String? get() = entries.maxByOrNull { it.date }?.date
}

class IncomeViewModel : ViewModel() {
    private val api = AppContainer.financeApi
    private val _state = MutableStateFlow(IncomeUiState())
    val state: StateFlow<IncomeUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (year, month) = DateUtils.currentYearMonth()
                _state.update {
                    it.copy(
                        isLoading = false,
                        entries = api.fetchIncomes(year, month),
                        accounts = api.fetchAccounts(),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun create(date: String, amount: Double, accountId: Int, note: String?, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                api.createIncome(date, amount, accountId, note)
                load()
                onSuccess()
            } catch (_: Exception) {
            }
        }
    }

    fun update(entry: IncomeEntry, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val accountId = entry.accountId ?: return@launch
            try {
                api.updateIncome(entry.id, entry.date, entry.amount, accountId, entry.note)
                load()
                onSuccess()
            } catch (_: Exception) {
            }
        }
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
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var accountId by remember(state.accounts) {
        mutableStateOf(DefaultAccountPicker.pick(state.accounts)?.id ?: 0)
    }
    var editing by remember { mutableStateOf<IncomeEntry?>(null) }
    var toDelete by remember { mutableStateOf<IncomeEntry?>(null) }
    var accountExpanded by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf("") }

    LaunchedEffect(state.accounts) {
        if (accountId == 0) {
            DefaultAccountPicker.pick(state.accounts)?.id?.let { accountId = it }
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }

    if (toDelete != null) {
        androidx.compose.material3.AlertDialog(
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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Income") },
                actions = { SettingsMenu() },
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
                    Text("Track money coming in this month.", color = AppColors.Muted)
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
                    }
                }
                item {
                    FinanceCard {
                        Text(if (editing == null) "Quick add" else "Edit income", fontWeight = FontWeight.SemiBold)
                        val initialDateMillis = remember(editing) {
                            editing?.date?.let { DateUtils.parseLocalIsoDate(it) }
                                ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                                ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }
                        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
                        LaunchedEffect(initialDateMillis) {
                            dateState.selectedDateMillis = initialDateMillis
                        }
                        DatePicker(state = dateState)
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                        )
                        ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = it }) {
                            OutlinedTextField(
                                value = state.accounts.firstOrNull { it.id == accountId }?.name.orEmpty(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Account") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                            )
                            ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                                state.accounts.forEach { account ->
                                    DropdownMenuItem(
                                        text = { Text(account.name) },
                                        onClick = { accountId = account.id; accountExpanded = false },
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (formError.isNotBlank()) {
                            Text(formError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                formError = ""
                                val amount = amountText.replace(",", ".").toDoubleOrNull()
                                val dateMillis = dateState.selectedDateMillis
                                if (amount == null || amount <= 0.0 || dateMillis == null || accountId == 0) {
                                    formError = "Enter an amount and choose an account."
                                    return@Button
                                }
                                val date = DateUtils.localIsoDate(
                                    Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
                                )
                                val clearForm = {
                                    amountText = ""
                                    note = ""
                                    editing = null
                                    formError = ""
                                }
                                if (editing == null) {
                                    viewModel.create(date, amount, accountId, note.ifBlank { null }, onSuccess = clearForm)
                                } else {
                                    viewModel.update(
                                        editing!!.copy(
                                            date = date,
                                            amount = amount,
                                            accountId = accountId,
                                            note = note.ifBlank { null },
                                            accountName = state.accounts.firstOrNull { it.id == accountId }?.name,
                                        ),
                                        onSuccess = clearForm,
                                    )
                                }
                            }) { Text(if (editing == null) "Add income" else "Save changes") }
                            if (editing != null) {
                                TextButton(onClick = { editing = null; amountText = ""; note = ""; formError = "" }) {
                                    Text("Cancel edit")
                                }
                            }
                        }
                    }
                }
                items(state.entries, key = { it.id }) { entry ->
                    IncomeRow(
                        entry = entry,
                        currency = currency,
                        onEdit = {
                            editing = entry
                            amountText = entry.amount.toString()
                            note = entry.note.orEmpty()
                            accountId = entry.accountId ?: accountId
                        },
                        onDelete = { toDelete = entry },
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeRow(entry: IncomeEntry, currency: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    FinanceCard {
        ListItem(
            headlineContent = { Text(CurrencyFormatter.format(entry.amount, currency), fontWeight = FontWeight.SemiBold) },
            supportingContent = {
                Column {
                    Text(entry.accountName ?: "Account")
                    Text(DateUtils.formatShortDate(entry.date), color = AppColors.Muted, style = MaterialTheme.typography.bodySmall)
                    entry.note?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            },
            trailingContent = {
                IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { expanded = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            },
        )
    }
}
