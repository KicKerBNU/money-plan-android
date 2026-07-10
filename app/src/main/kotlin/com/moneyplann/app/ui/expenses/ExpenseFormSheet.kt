package com.moneyplann.app.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.data.models.Category
import com.moneyplann.app.data.models.Expense
import com.moneyplann.app.data.models.RecurrenceFrequency
import com.moneyplann.app.data.models.RecurrenceSave
import com.moneyplann.app.data.models.RecurringExpense
import com.moneyplann.app.data.models.displayLabel
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.ExpenseTheme
import com.moneyplann.app.util.DateUtils
import com.moneyplann.app.util.DefaultAccountPicker
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormSheet(
    accounts: List<Account>,
    categories: List<Category>,
    editing: Expense? = null,
    linkedRecurring: RecurringExpense? = null,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (
        date: String,
        amount: Double,
        categoryId: Int,
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
    var categoryId by remember(editing, categories) {
        mutableStateOf(editing?.categoryId ?: categories.firstOrNull()?.id ?: 0)
    }
    var accountId by remember(editing, accounts) {
        mutableStateOf(editing?.accountId ?: DefaultAccountPicker.pick(accounts)?.id ?: 0)
    }
    var note by remember(editing) { mutableStateOf(editing?.note.orEmpty()) }
    var isRecurring by remember(editing) { mutableStateOf(false) }
    var recurrenceFrequency by remember(editing) { mutableStateOf(RecurrenceFrequency.monthly) }
    var loadedRecurring by remember(editing?.recurringExpenseId) { mutableStateOf<RecurringExpense?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val activeRecurring = linkedRecurring ?: loadedRecurring
    val showsRecurrenceSection = editing == null || editing.recurringExpenseId != null || activeRecurring != null

    LaunchedEffect(editing?.recurringExpenseId, linkedRecurring) {
        linkedRecurring?.let {
            isRecurring = it.active
            recurrenceFrequency = it.frequency
        }
        val recurringId = editing?.recurringExpenseId ?: return@LaunchedEffect
        try {
            val item = AppContainer.financeApi.fetchRecurringExpense(recurringId)
            loadedRecurring = item
            isRecurring = item.active
            recurrenceFrequency = item.frequency
        } catch (_: Exception) {
            loadedRecurring = null
            if (editing.recurringExpenseId != null) {
                isRecurring = false
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
        color = ExpenseTheme.ScreenBackground,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (editing == null) "New expense" else "Edit expense") },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = AppColors.ActionBlue)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            val amount = amountText.replace(",", ".").toDoubleOrNull()
                            val dateMillis = dateState.selectedDateMillis
                            if (amount == null || dateMillis == null || categoryId == 0 || accountId == 0) {
                                errorMessage = "Fill in all required fields."
                                return@TextButton
                            }
                            val date = DateUtils.localIsoDate(DateUtils.localDateFromDatePickerUtcMillis(dateMillis))
                            val recurrence = when {
                                editing == null && isRecurring ->
                                    RecurrenceSave(isCreate = true, enabled = true, frequency = recurrenceFrequency)
                                editing != null && showsRecurrenceSection ->
                                    RecurrenceSave(isCreate = false, enabled = isRecurring, frequency = recurrenceFrequency)
                                else -> null
                            }
                            onSave(date, amount, categoryId, accountId, note.ifBlank { null }, recurrence)
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
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) { Text("Pick", color = AppColors.ActionBlue) }
                    },
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
                )

                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                    OutlinedTextField(
                        value = categories.firstOrNull { it.id == categoryId }?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categories.forEach { category ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = { categoryId = category.id; categoryExpanded = false },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = accountExpanded, onExpandedChange = { accountExpanded = it }) {
                    OutlinedTextField(
                        value = accounts.firstOrNull { it.id == accountId }?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(accountExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = accountExpanded, onDismissRequest = { accountExpanded = false }) {
                        accounts.forEach { account ->
                            androidx.compose.material3.DropdownMenuItem(
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
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                )

                if (showsRecurrenceSection) {
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text("Recurrence", style = MaterialTheme.typography.titleSmall)
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text("Repeat this expense")
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
                                    androidx.compose.material3.DropdownMenuItem(
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
                    Text(
                        when {
                            editing != null && isRecurring ->
                                "Changes apply to the linked recurring template."
                            isRecurring ->
                                "A recurring template will be created for future payments."
                            editing?.recurringExpenseId != null ->
                                "Recurrence is paused. Turn on to schedule future payments again."
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
                        Text("Delete expense", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
