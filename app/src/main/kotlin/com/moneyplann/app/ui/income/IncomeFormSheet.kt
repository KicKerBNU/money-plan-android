package com.moneyplann.app.ui.income

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.data.models.IncomeEntry
import com.moneyplann.app.data.models.RecurrenceFrequency
import com.moneyplann.app.data.models.RecurrenceSave
import com.moneyplann.app.data.models.RecurringIncome
import com.moneyplann.app.data.models.displayLabel
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.IncomeTheme
import com.moneyplann.app.util.DateUtils
import com.moneyplann.app.util.DefaultAccountPicker
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeFormSheet(
    accounts: List<Account>,
    editing: IncomeEntry? = null,
    linkedRecurring: RecurringIncome? = null,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (
        date: String,
        amount: Double,
        accountId: Int,
        note: String?,
        recurrence: RecurrenceSave?,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialDateMillis = remember(editing) {
        val date = editing?.date?.let(DateUtils::parseLocalIsoDate) ?: LocalDate.now()
        DateUtils.datePickerUtcMillis(date)
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    var amountText by remember(editing) { mutableStateOf(editing?.amount?.toString().orEmpty()) }
    var accountId by remember(editing, accounts) {
        mutableStateOf(
            editing?.accountId?.takeIf { it > 0 }
                ?: DefaultAccountPicker.pick(accounts)?.id
                ?: 0,
        )
    }
    var note by remember(editing) { mutableStateOf(editing?.note.orEmpty()) }
    var isRecurring by remember(editing) { mutableStateOf(false) }
    var recurrenceFrequency by remember(editing) { mutableStateOf(RecurrenceFrequency.monthly) }
    var loadedRecurring by remember(editing?.recurringIncomeId) { mutableStateOf<RecurringIncome?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var accountExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val resolvedRecurringId = editing?.recurringIncomeId ?: linkedRecurring?.id ?: loadedRecurring?.id

    LaunchedEffect(editing?.id, linkedRecurring?.id) {
        if (editing?.recurringIncomeId != null) {
            isRecurring = true
        } else {
            linkedRecurring?.let {
                isRecurring = it.active
                recurrenceFrequency = it.frequency
            }
        }

        val recurringId = editing?.recurringIncomeId ?: linkedRecurring?.id ?: return@LaunchedEffect
        try {
            val item = AppContainer.financeApi.fetchRecurringIncome(recurringId)
            loadedRecurring = item
            isRecurring = item.active
            recurrenceFrequency = item.frequency
        } catch (_: Exception) {
            loadedRecurring = linkedRecurring
            if (editing?.recurringIncomeId != null) {
                isRecurring = true
            }
        }
    }

    LaunchedEffect(initialDateMillis) {
        dateState.selectedDateMillis = initialDateMillis
    }

    val selectedDateLabel = remember(dateState.selectedDateMillis) {
        dateState.selectedDateMillis?.let { millis ->
            DateUtils.formatShortDate(
                DateUtils.localIsoDate(DateUtils.localDateFromDatePickerUtcMillis(millis)),
            )
        }.orEmpty()
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = IncomeTheme.FormSheetBackground,
    ) {
        Scaffold(
            containerColor = IncomeTheme.FormSheetBackground,
            topBar = {
                TopAppBar(
                    title = { Text(if (editing == null) "New income" else "Edit income") },
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
                            val date = DateUtils.localIsoDate(DateUtils.localDateFromDatePickerUtcMillis(dateMillis))
                            val recurrence = when {
                                editing == null && isRecurring ->
                                    RecurrenceSave(isCreate = true, enabled = true, frequency = recurrenceFrequency)
                                editing != null && isRecurring ->
                                    RecurrenceSave(isCreate = false, enabled = true, frequency = recurrenceFrequency)
                                editing != null && resolvedRecurringId != null ->
                                    RecurrenceSave(isCreate = false, enabled = false, frequency = recurrenceFrequency)
                                else -> null
                            }
                            onSave(date, amount, accountId, note.ifBlank { null }, recurrence)
                        }) { Text("Save", color = AppColors.ActionBlue) }
                    },
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
                    value = selectedDateLabel,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                )

                ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = it }) {
                    OutlinedTextField(
                        value = accounts.firstOrNull { it.id == accountId }?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Bank Accounts") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                        accounts.forEach { account ->
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
                    placeholder = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Recurrence", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Repeat this income")
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }
                if (isRecurring) {
                    ExposedDropdownMenuBox(
                        expanded = frequencyExpanded,
                        onExpandedChange = { frequencyExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = recurrenceFrequency.displayLabel(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Frequency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(frequencyExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = frequencyExpanded,
                            onDismissRequest = { frequencyExpanded = false },
                        ) {
                            RecurrenceFrequency.entries.forEach { frequency ->
                                DropdownMenuItem(
                                    text = { Text(frequency.displayLabel()) },
                                    onClick = {
                                        recurrenceFrequency = frequency
                                        frequencyExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                if (editing != null || isRecurring || resolvedRecurringId != null) {
                    Text(
                        when {
                            editing != null && isRecurring ->
                                "Changes apply to the linked recurring template."
                            isRecurring ->
                                "A recurring template will be created for future deposits."
                            resolvedRecurringId != null ->
                                "Recurrence is paused. Turn on to schedule future deposits again."
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.Muted,
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
                }

                if (editing != null && onDelete != null) {
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Delete income", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
