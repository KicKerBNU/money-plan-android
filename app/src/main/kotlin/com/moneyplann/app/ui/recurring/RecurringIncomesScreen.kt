package com.moneyplann.app.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.data.models.RecurrenceFrequency
import com.moneyplann.app.data.models.RecurringIncome
import com.moneyplann.app.data.models.displayLabel
import com.moneyplann.app.ui.components.EmptyStateView
import com.moneyplann.app.ui.components.ErrorStateView
import com.moneyplann.app.ui.components.IncomeIconView
import com.moneyplann.app.ui.components.LoadingStateView
import com.moneyplann.app.ui.components.ScreenHeader
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.IncomeTheme
import com.moneyplann.app.ui.theme.RecurringTheme
import com.moneyplann.app.util.CurrencyFormatter
import com.moneyplann.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringIncomesScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringIncomesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val currency = AppContainer.moneyPreferences.activeCurrency
    var editingItem by remember { mutableStateOf<RecurringIncome?>(null) }
    var itemToDelete by remember { mutableStateOf<RecurringIncome?>(null) }
    val screenBackground = RecurringTheme.ScreenBackground

    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = screenBackground,
            topBar = {
                ScreenHeader(
                    title = "Recurring income",
                    backgroundColor = screenBackground,
                    actions = {
                        TextButton(onClick = onDismiss) {
                            Text("Back", color = AppColors.ActionBlue)
                        }
                    },
                )
            },
        ) { padding ->
            when {
                state.isLoading -> LoadingStateView(Modifier.padding(padding))
                state.errorMessage != null -> ErrorStateView(state.errorMessage!!, Modifier.padding(padding))
                state.items.isEmpty() -> EmptyStateView(
                    title = "No recurring income",
                    message = "Mark an income entry as recurring when you add it, or link one when editing an existing entry.",
                    modifier = Modifier.padding(padding).fillMaxSize(),
                )
                else -> PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { viewModel.load() },
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        items(state.items, key = { it.id }) { item ->
                            RecurringIncomeCard(
                                item = item,
                                currency = currency,
                                onToggleActive = { active -> viewModel.setActive(item, active) },
                                onEdit = { editingItem = item },
                                onDelete = { itemToDelete = item },
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }

        if (editingItem != null) {
            RecurringIncomeEditSheet(
                modifier = Modifier.fillMaxSize(),
                item = editingItem!!,
                accounts = state.accounts,
                onDismiss = { editingItem = null },
                onSave = { updated ->
                    viewModel.update(updated)
                    editingItem = null
                },
            )
        }
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete recurring income?") },
            text = { Text("This removes the recurring template. Existing income entries are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(itemToDelete!!)
                    itemToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RecurringIncomeCard(
    item: RecurringIncome,
    currency: String,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val title = item.note?.takeIf { it.isNotBlank() } ?: item.accountName
    val subtitle = buildString {
        append(item.accountName)
        if (item.active) {
            append(" · Next due ${DateUtils.formatShortDate(item.nextDate)}")
        }
    }
    val formattedAmount = CurrencyFormatter.format(item.amount, currency)
    val amount = if (item.active) "+$formattedAmount" else formattedAmount

    RecurringItemCard(
        icon = {
            IncomeIconView(
                label = title,
                accentColor = IncomeTheme.IconAccent,
                chipBaseSurface = RecurringTheme.CardBackground,
            )
        },
        title = title,
        subtitle = subtitle,
        amount = amount,
        amountColor = if (item.active) AppColors.Positive else AppColors.Muted,
        frequency = item.frequency,
        active = item.active,
        onToggleActive = onToggleActive,
        onEdit = onEdit,
        onDelete = onDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringIncomeEditSheet(
    item: RecurringIncome,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (RecurringIncome) -> Unit,
    modifier: Modifier = Modifier,
) {
    var amountText by remember(item) { mutableStateOf(item.amount.toString()) }
    var accountId by remember(item) { mutableStateOf(item.accountId) }
    var note by remember(item) { mutableStateOf(item.note.orEmpty()) }
    var frequency by remember(item) { mutableStateOf(item.frequency) }
    var isActive by remember(item) { mutableStateOf(item.active) }
    var accountExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val startDate = remember(item) { DateUtils.parseLocalIsoDate(item.startDate) }
    val initialDateMillis = remember(item) {
        DateUtils.datePickerUtcMillis(startDate ?: java.time.LocalDate.now())
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    LaunchedEffect(initialDateMillis) {
        dateState.selectedDateMillis = initialDateMillis
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = RecurringTheme.FormSheetBackground,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = RecurringTheme.FormSheetBackground,
            topBar = {
                TopAppBar(
                    title = { Text("Edit recurring income") },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = AppColors.ActionBlue)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            val amount = amountText.replace(",", ".").toDoubleOrNull()
                            val dateMillis = dateState.selectedDateMillis
                            if (amount == null || dateMillis == null || accountId == 0) {
                                errorMessage = "Fill in all required fields."
                                return@TextButton
                            }
                            val startIso = DateUtils.localIsoDate(DateUtils.localDateFromDatePickerUtcMillis(dateMillis))
                            val accountName = accounts.firstOrNull { it.id == accountId }?.name.orEmpty()
                            onSave(
                                item.copy(
                                    amount = amount,
                                    accountId = accountId,
                                    accountName = accountName,
                                    note = note.ifBlank { null },
                                    frequency = frequency,
                                    startDate = startIso,
                                    nextDate = startIso,
                                    active = isActive,
                                ),
                            )
                        }) {
                            Text("Save", color = AppColors.ActionBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = RecurringTheme.FormSheetBackground,
                    ),
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                AccountFrequencyPickers(
                    accounts = accounts,
                    accountId = accountId,
                    frequency = frequency,
                    onAccountChange = { accountId = it },
                    onFrequencyChange = { frequency = it },
                    accountExpanded = accountExpanded,
                    frequencyExpanded = frequencyExpanded,
                    onAccountExpandedChange = { accountExpanded = it },
                    onFrequencyExpandedChange = { frequencyExpanded = it },
                )
                Text("Start date", style = MaterialTheme.typography.labelLarge, color = AppColors.PrimaryLabel)
                DatePicker(state = dateState)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Active", color = AppColors.PrimaryLabel)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountFrequencyPickers(
    accounts: List<Account>,
    accountId: Int,
    frequency: RecurrenceFrequency,
    onAccountChange: (Int) -> Unit,
    onFrequencyChange: (RecurrenceFrequency) -> Unit,
    accountExpanded: Boolean,
    frequencyExpanded: Boolean,
    onAccountExpandedChange: (Boolean) -> Unit,
    onFrequencyExpandedChange: (Boolean) -> Unit,
) {
    ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = onAccountExpandedChange) {
        OutlinedTextField(
            value = accounts.firstOrNull { it.id == accountId }?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Account") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { onAccountExpandedChange(false) }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = { onAccountChange(account.id); onAccountExpandedChange(false) },
                )
            }
        }
    }
    ExposedDropdownMenuBox(expanded = frequencyExpanded, onExpandedChange = onFrequencyExpandedChange) {
        OutlinedTextField(
            value = frequency.displayLabel(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Frequency") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(frequencyExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = frequencyExpanded, onDismissRequest = { onFrequencyExpandedChange(false) }) {
            RecurrenceFrequency.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayLabel()) },
                    onClick = { onFrequencyChange(option); onFrequencyExpandedChange(false) },
                )
            }
        }
    }
}
