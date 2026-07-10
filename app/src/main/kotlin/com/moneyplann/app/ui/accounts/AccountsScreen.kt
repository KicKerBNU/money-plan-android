package com.moneyplann.app.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.ui.components.AccountIconView
import com.moneyplann.app.ui.components.EmptyStateView
import com.moneyplann.app.ui.components.ErrorStateView
import com.moneyplann.app.ui.components.LoadingStateView
import com.moneyplann.app.ui.components.ScreenHeader
import com.moneyplann.app.ui.components.accountAccentColor
import com.moneyplann.app.ui.components.accountIconForKind
import com.moneyplann.app.ui.components.accountVisualKind
import com.moneyplann.app.ui.settings.SettingsMenu
import com.moneyplann.app.ui.theme.AccountsTheme
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

    fun create(name: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                api.createAccount(name)
                load()
                onSuccess()
            } catch (_: Exception) {
            }
        }
    }

    fun rename(id: Int, name: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                api.updateAccount(id, name)
                load()
                onSuccess()
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

    fun setDefault(id: Int) {
        viewModelScope.launch {
            try {
                api.setAccountDefault(id)
                load()
            } catch (_: Exception) {
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(modifier: Modifier = Modifier, viewModel: AccountsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val currency = AppContainer.moneyPreferences.activeCurrency
    val screenBackground = AccountsTheme.ScreenBackground
    var showAddSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var toDelete by remember { mutableStateOf<Account?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete account?") },
            text = { Text("This account and its related data will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(toDelete!!.id)
                    if (editingAccount?.id == toDelete?.id) {
                        editingAccount = null
                    }
                    toDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }

    Box(modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = screenBackground,
            topBar = {
                ScreenHeader(
                    title = "Accounts",
                    subtitle = "Manage where money lives.",
                    backgroundColor = screenBackground,
                    actions = {
                        IconButton(onClick = { showAddSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add account")
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
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        if (state.accounts.isEmpty()) {
                            item {
                                EmptyStateView(
                                    title = "No accounts yet",
                                    message = "Add a bank account, cash wallet, or card to track where your money lives.",
                                    actionTitle = "Add account",
                                    onAction = { showAddSheet = true },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        } else {
                            item {
                                FeaturedAccountsCarousel(
                                    accounts = state.accounts,
                                    currency = currency,
                                    onAccountClick = { editingAccount = it },
                                )
                            }
                            item {
                                AllAccountsSection(
                                    accounts = state.accounts,
                                    currency = currency,
                                    onEdit = { editingAccount = it },
                                    onDelete = { toDelete = it },
                                    onSetDefault = { viewModel.setDefault(it.id) },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }

        if (showAddSheet) {
            AccountFormSheet(
                modifier = Modifier.fillMaxSize(),
                onDismiss = { showAddSheet = false },
                onSave = { name ->
                    viewModel.create(name) { showAddSheet = false }
                },
            )
        }

        editingAccount?.let { account ->
            AccountFormSheet(
                modifier = Modifier.fillMaxSize(),
                editing = account,
                onDismiss = { editingAccount = null },
                onDelete = { toDelete = account },
                onSave = { name ->
                    viewModel.rename(account.id, name) { editingAccount = null }
                },
            )
        }
    }
}

@Composable
private fun FeaturedAccountsCarousel(
    accounts: List<Account>,
    currency: String,
    onAccountClick: (Account) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(accounts, key = { it.id }) { account ->
            FeaturedAccountCard(
                account = account,
                currency = currency,
                onClick = { onAccountClick(account) },
            )
        }
    }
}

@Composable
private fun FeaturedAccountCard(
    account: Account,
    currency: String,
    onClick: () -> Unit,
) {
    val kind = accountVisualKind(account.name)
    val accent = accountAccentColor(kind)

    Box(
        modifier = Modifier
            .width(168.dp)
            .height(132.dp)
            .shadow(
                elevation = if (AccountsTheme.isDark) 0.dp else 4.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f),
            )
            .clip(RoundedCornerShape(18.dp))
            .background(AccountsTheme.FeaturedCardGradient)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccountsTheme.FeaturedIconScrim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = accountIconForKind(kind),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (account.isDefault) {
                DefaultBadge()
            }
        }
        Column(
            Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                account.name,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                CurrencyFormatter.format(account.currentBalance, currency),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DefaultBadge() {
    Text(
        "DEFAULT",
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AccountsTheme.DefaultBadgeBackground)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = AccountsTheme.DefaultBadgeText,
        letterSpacing = 0.4.sp,
    )
}

@Composable
private fun AllAccountsSection(
    accounts: List<Account>,
    currency: String,
    onEdit: (Account) -> Unit,
    onDelete: (Account) -> Unit,
    onSetDefault: (Account) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "ALL ACCOUNTS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.Muted,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = AccountsTheme.ListElevation,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(AccountsTheme.ListGroupBackground)
                .border(1.dp, AccountsTheme.ListBorderColor, RoundedCornerShape(16.dp)),
        ) {
            accounts.forEachIndexed { index, account ->
                AccountListRow(
                    account = account,
                    currency = currency,
                    onEdit = { onEdit(account) },
                    onDelete = { onDelete(account) },
                    onSetDefault = { onSetDefault(account) },
                )
                if (index < accounts.lastIndex) {
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
private fun AccountListRow(
    account: Account,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
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
            AccountIconView(
                accountName = account.name,
                chipBaseSurface = AccountsTheme.ListGroupBackground,
            )
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    account.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (account.isDefault) {
                    DefaultBadge()
                }
            }
            Text(
                CurrencyFormatter.format(account.currentBalance, currency),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Account options")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (!account.isDefault) {
                    DropdownMenuItem(
                        text = { Text("Set as default") },
                        onClick = { menuExpanded = false; onSetDefault() },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    )
                }
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
