package com.moneyplann.app.ui.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyplann.app.AppContainer
import com.moneyplann.app.ui.categories.CategoriesScreen
import com.moneyplann.app.data.prefs.ThemePreference
import com.moneyplann.app.ui.components.ScreenHeader
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.SettingsTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private sealed interface SettingsRoute {
    data object Main : SettingsRoute
    data object Theme : SettingsRoute
    data object Currency : SettingsRoute
    data object Categories : SettingsRoute
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val categoryCount: Int = 0,
    val activeRecurringExpenses: Int = 0,
    val activeRecurringIncomes: Int = 0,
)

class SettingsViewModel : ViewModel() {
    private val api = AppContainer.financeApi
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val categories = runCatching { api.fetchCategories() }.getOrDefault(emptyList())
                val recurringExpenses = runCatching { api.fetchRecurringExpenses() }.getOrDefault(emptyList())
                val recurringIncomes = runCatching { api.fetchRecurringIncomes() }.getOrDefault(emptyList())
                _state.update {
                    it.copy(
                        isLoading = false,
                        categoryCount = categories.size,
                        activeRecurringExpenses = recurringExpenses.count { item -> item.active },
                        activeRecurringIncomes = recurringIncomes.count { item -> item.active },
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    onOpenRecurringExpenses: () -> Unit,
    onOpenRecurringIncomes: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    var route by remember { mutableStateOf<SettingsRoute>(SettingsRoute.Main) }
    val screenBackground = SettingsTheme.ScreenBackground

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(route) {
        if (route == SettingsRoute.Main) viewModel.load()
    }

    if (route == SettingsRoute.Categories) {
        CategoriesScreen(
            modifier = modifier.fillMaxSize(),
            onBack = { route = SettingsRoute.Main },
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = screenBackground,
        topBar = {
            SettingsTopBar(
                title = when (route) {
                    SettingsRoute.Main -> "Settings"
                    SettingsRoute.Theme -> "Theme"
                    SettingsRoute.Currency -> "Currency"
                    SettingsRoute.Categories -> "Categories"
                },
                backgroundColor = screenBackground,
                onBack = {
                    if (route == SettingsRoute.Main) onDismiss() else route = SettingsRoute.Main
                },
            )
        },
    ) { padding ->
        when (route) {
            SettingsRoute.Main -> SettingsMainContent(
                modifier = Modifier.padding(padding),
                viewModel = viewModel,
                onDismiss = onDismiss,
                onNavigate = { route = it },
                onOpenRecurringExpenses = onOpenRecurringExpenses,
                onOpenRecurringIncomes = onOpenRecurringIncomes,
            )
            SettingsRoute.Theme -> ThemeSettingsContent(Modifier.padding(padding))
            SettingsRoute.Currency -> CurrencySettingsContent(Modifier.padding(padding))
            SettingsRoute.Categories -> Unit
        }
    }
}

@Composable
private fun SettingsTopBar(
    title: String,
    backgroundColor: Color,
    onBack: () -> Unit,
) {
    ScreenHeader(
        title = title,
        backgroundColor = backgroundColor,
        actions = {
            TextButton(onClick = onBack) {
                Text("Back", color = AppColors.ActionBlue)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsMainContent(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onNavigate: (SettingsRoute) -> Unit,
    onOpenRecurringExpenses: () -> Unit,
    onOpenRecurringIncomes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val themePrefs = AppContainer.themePreferences
    val moneyPrefs = AppContainer.moneyPreferences
    val notificationPrefs = AppContainer.notificationPreferences
    val theme by themePrefs.preference.collectAsState()
    val currency by moneyPrefs.currency.collectAsState()
    val notificationsEnabled by notificationPrefs.enabled.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete your account?") },
            text = {
                Text("This permanently deletes your Money Plan account, all expenses, income, accounts, and categories. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            try {
                                AppContainer.authRepository.deleteAccount(AppContainer.financeApi)
                                showDeleteConfirm = false
                                onDismiss()
                            } finally {
                                isDeleting = false
                            }
                        }
                    },
                ) {
                    Text("Delete account", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = { viewModel.load() },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { Spacer(Modifier.size(4.dp)) }

            item {
                SettingsSection(title = "APPEARANCE") {
                    SettingsNavigationRow(
                        icon = Icons.Outlined.DarkMode,
                        title = "Theme",
                        value = theme.displayLabel(),
                        onClick = { onNavigate(SettingsRoute.Theme) },
                    )
                    SettingsGroupDivider()
                    SettingsToggleRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { notificationPrefs.setEnabled(enabled) }
                        },
                    )
                }
            }

            item {
                SettingsSection(title = "PREFERENCES") {
                    SettingsNavigationRow(
                        icon = Icons.Default.AttachMoney,
                        title = "Currency",
                        value = currencyDisplayLabel(currency, moneyPrefs.autoCurrency),
                        onClick = { onNavigate(SettingsRoute.Currency) },
                    )
                    SettingsGroupDivider()
                    SettingsNavigationRow(
                        icon = Icons.Default.Sell,
                        title = "Categories",
                        value = if (state.isLoading) "…" else state.categoryCount.toString(),
                        onClick = { onNavigate(SettingsRoute.Categories) },
                    )
                }
            }

            item {
                SettingsSection(title = "RECURRING") {
                    SettingsNavigationRow(
                        icon = Icons.Default.EventRepeat,
                        title = "Recurring expenses",
                        value = activeCountLabel(state.activeRecurringExpenses, state.isLoading),
                        onClick = onOpenRecurringExpenses,
                    )
                    SettingsGroupDivider()
                    SettingsNavigationRow(
                        icon = Icons.Default.EventRepeat,
                        title = "Recurring income",
                        value = activeCountLabel(state.activeRecurringIncomes, state.isLoading),
                        onClick = onOpenRecurringIncomes,
                    )
                }
            }

            item {
                SettingsSection(title = "ACCOUNT") {
                    SettingsNavigationRow(
                        icon = Icons.Default.Delete,
                        title = "Delete account",
                        iconTint = MaterialTheme.colorScheme.error,
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showDeleteConfirm = true },
                        enabled = !isDeleting,
                    )
                    SettingsGroupDivider()
                    SettingsActionRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Log out",
                        onClick = { AppContainer.authRepository.signOut() },
                    )
                }
            }

            item { Spacer(Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun ThemeSettingsContent(modifier: Modifier = Modifier) {
    val themePrefs = AppContainer.themePreferences
    val theme by themePrefs.preference.collectAsState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Spacer(Modifier.size(4.dp)) }
        item {
            SettingsSection(title = null) {
                ThemePreference.entries.forEachIndexed { index, option ->
                    SettingsSelectionRow(
                        title = option.displayLabel(),
                        selected = theme == option,
                        onClick = { scope.launch { themePrefs.setPreference(option) } },
                    )
                    if (index < ThemePreference.entries.lastIndex) {
                        SettingsGroupDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencySettingsContent(modifier: Modifier = Modifier) {
    val moneyPrefs = AppContainer.moneyPreferences
    val currency by moneyPrefs.currency.collectAsState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Spacer(Modifier.size(4.dp)) }
        item {
            SettingsSection(title = null) {
                SettingsSelectionRow(
                    title = "Auto (${moneyPrefs.autoCurrency})",
                    selected = currency == null,
                    onClick = { scope.launch { moneyPrefs.setCurrency(null) } },
                )
                moneyPrefs.supportedCurrencies.forEach { code ->
                    SettingsGroupDivider()
                    SettingsSelectionRow(
                        title = code,
                        selected = currency == code,
                        onClick = { scope.launch { moneyPrefs.setCurrency(code) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String?,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Muted,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = SettingsTheme.GroupElevation,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(SettingsTheme.GroupBackground)
                .border(1.dp, SettingsTheme.GroupBorderColor, RoundedCornerShape(16.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    iconTint: Color = SettingsTheme.IconTint,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = titleColor, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = AppColors.Muted,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = SettingsTheme.IconTint, modifier = Modifier.size(22.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = SettingsTheme.IconTint, modifier = Modifier.size(22.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingsSelectionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsGroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )
}

private fun ThemePreference.displayLabel(): String = when (this) {
    ThemePreference.SYSTEM -> "System"
    ThemePreference.LIGHT -> "Light"
    ThemePreference.DARK -> "Dark"
}

private fun currencyDisplayLabel(currency: String?, autoCurrency: String): String =
    currency?.let { code -> code } ?: "Auto ($autoCurrency)"

private fun activeCountLabel(count: Int, isLoading: Boolean): String =
    if (isLoading) "…" else "$count active"
