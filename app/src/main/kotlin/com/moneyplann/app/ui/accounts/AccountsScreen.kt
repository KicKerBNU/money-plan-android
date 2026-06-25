package com.moneyplann.app.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.ui.components.ErrorStateView
import com.moneyplann.app.ui.components.FinanceCard
import com.moneyplann.app.ui.components.LoadingStateView
import com.moneyplann.app.ui.settings.SettingsMenu
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.util.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val accounts: List<Account> = emptyList(),
)

class AccountsViewModel : ViewModel() {
    private val api = AppContainer.financeApi
    private val _state = MutableStateFlow(AccountsUiState())
    val state: StateFlow<AccountsUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                _state.update { it.copy(isLoading = false, accounts = api.fetchAccounts()) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun create(name: String) {
        viewModelScope.launch {
            try {
                api.createAccount(name)
                load()
            } catch (_: Exception) {
            }
        }
    }

    fun rename(id: Int, name: String) {
        viewModelScope.launch {
            try {
                api.updateAccount(id, name)
                load()
            } catch (_: Exception) {
            }
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            try {
                api.deleteAccount(id)
                load()
            } catch (_: Exception) {
            }
        }
    }

    fun moveAndSetDefault(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val ordered = _state.value.accounts.toMutableList()
            val item = ordered.removeAt(fromIndex)
            ordered.add(if (toIndex > fromIndex) toIndex - 1 else toIndex, item)
            val snapshot = _state.value.accounts
            _state.update { it.copy(accounts = ordered) }
            val first = ordered.firstOrNull() ?: return@launch
            try {
                api.setAccountDefault(first.id)
                load()
            } catch (_: Exception) {
                _state.update { it.copy(accounts = snapshot) }
                load()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(modifier: Modifier = Modifier, viewModel: AccountsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val currency = AppContainer.moneyPreferences.activeCurrency
    var newName by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<Account?>(null) }
    var renameText by remember { mutableStateOf("") }
    var toDelete by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    if (renaming != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename account") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text("Name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rename(renaming!!.id, renameText)
                    renaming = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } },
        )
    }

    if (toDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete account?") },
            text = { Text("This account and its related data will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(toDelete!!.id)
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
                title = { Text("Accounts") },
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
                item { Text("Manage where money lives.", color = AppColors.Muted) }
                item {
                    FinanceCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Account name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            TextButton(
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        viewModel.create(newName.trim())
                                        newName = ""
                                    }
                                },
                                enabled = newName.isNotBlank(),
                            ) { Text("Add") }
                        }
                    }
                }
                itemsIndexed(state.accounts, key = { _, account -> account.id }) { index, account ->
                    AccountRow(
                        account = account,
                        currency = currency,
                        onRename = { renaming = account; renameText = account.name },
                        onDelete = { toDelete = account },
                        onMoveUp = { if (index > 0) viewModel.moveAndSetDefault(index, index - 1) },
                        onMoveDown = {
                            if (index < state.accounts.lastIndex) viewModel.moveAndSetDefault(index, index + 2)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    account: Account,
    currency: String,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    FinanceCard {
        ListItem(
            leadingContent = { Icon(Icons.Default.DragHandle, contentDescription = "Reorder") },
            headlineContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(account.name, fontWeight = FontWeight.SemiBold)
                    if (account.isDefault) AssistChip(onClick = {}, label = { Text("Default") })
                }
            },
            supportingContent = {
                Text("Balance: ${CurrencyFormatter.format(account.currentBalance, currency)}")
            },
            trailingContent = {
                IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Move up") }, onClick = { expanded = false; onMoveUp() })
                    DropdownMenuItem(text = { Text("Move down") }, onClick = { expanded = false; onMoveDown() })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { expanded = false; onRename() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            },
        )
    }
}
