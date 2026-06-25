package com.moneyplann.app.ui.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.data.models.Category
import com.moneyplann.app.data.models.Expense
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.util.DateUtils
import com.moneyplann.app.util.DefaultAccountPicker
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormSheet(
    accounts: List<Account>,
    categories: List<Category>,
    editing: Expense? = null,
    onDismiss: () -> Unit,
    onSave: (date: String, amount: Double, categoryId: Int, accountId: Int, note: String?) -> Unit,
) {
    val initialDate = editing?.date?.let { DateUtils.parseLocalIsoDate(it) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    )
    var amountText by remember(editing) { mutableStateOf(editing?.amount?.toString().orEmpty()) }
    var categoryId by remember(editing, categories) {
        mutableStateOf(editing?.categoryId ?: categories.firstOrNull()?.id ?: 0)
    }
    var accountId by remember(editing, accounts) {
        mutableStateOf(editing?.accountId ?: DefaultAccountPicker.pick(accounts)?.id ?: 0)
    }
    var note by remember(editing) { mutableStateOf(editing?.note.orEmpty()) }
    var errorMessage by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var accountExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing == null) "Add expense" else "Edit expense") },
                navigationIcon = { TextButton(onClick = onDismiss) { Text("Cancel") } },
                actions = {
                    TextButton(onClick = {
                        val amount = amountText.replace(",", ".").toDoubleOrNull()
                        val dateMillis = dateState.selectedDateMillis
                        if (amount == null || dateMillis == null || categoryId == 0 || accountId == 0) {
                            errorMessage = "Fill in all required fields."
                            return@TextButton
                        }
                        val date = DateUtils.localIsoDate(
                            Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
                        )
                        onSave(date, amount, categoryId, accountId, note.ifBlank { null })
                    }) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DatePicker(state = dateState)
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
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
                modifier = Modifier.fillMaxWidth(),
            )
            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
