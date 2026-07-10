package com.moneyplann.app.ui.income

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.data.models.IncomeEntry
import com.moneyplann.app.data.models.RecurrenceFrequency
import com.moneyplann.app.data.models.RecurrenceSave
import com.moneyplann.app.data.models.RecurringIncome
import com.moneyplann.app.ui.components.EmptyStateView
import com.moneyplann.app.ui.components.ErrorStateView
import com.moneyplann.app.ui.components.IncomeIconView
import com.moneyplann.app.ui.components.LoadingStateView
import com.moneyplann.app.ui.components.ScreenHeader
import com.moneyplann.app.ui.settings.SettingsMenu
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.IncomeTheme
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

    val sortedEntries: List<IncomeEntry>
        get() = entries.sortedByDescending { it.date }

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
                load()
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
    val screenBackground = IncomeTheme.ScreenBackground

    LaunchedEffect(Unit) { viewModel.load() }

    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete income?") },
            text = { Text("This income entry will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(toDelete!!)
                    if (editingEntry?.id == toDelete?.id) {
                        editingEntry = null
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
                    title = "Income",
                    backgroundColor = screenBackground,
                    actions = {
                        IconButton(onClick = { showAddSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add income")
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
                        item {
                            PeriodPicker(
                                periodLabel = state.periodLabel,
                                onPrev = { viewModel.shiftPeriod(-1) },
                                onNext = { viewModel.shiftPeriod(1) },
                            )
                        }
                        item {
                            TotalIncomeSummary(total = state.total, currency = currency)
                        }
                        if (state.sortedEntries.isEmpty()) {
                            item {
                                EmptyStateView(
                                    title = "No income this month",
                                    message = "Add salary, freelance payments, rent, or any other money coming in.",
                                    actionTitle = "Add income",
                                    onAction = { showAddSheet = true },
                                )
                            }
                        } else {
                            item {
                                RecentIncomeSection(
                                    entries = state.sortedEntries,
                                    currency = currency,
                                    onEdit = { editingEntry = it },
                                    onDelete = { toDelete = it },
                                )
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
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
                onDelete = { toDelete = entry },
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
private fun TotalIncomeSummary(total: Double, currency: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Total income",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.Muted,
        )
        Text(
            text = CurrencyFormatter.format(total, currency),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(IncomeTheme.TotalPillBackground)
                .padding(horizontal = 28.dp, vertical = 12.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = IncomeTheme.TotalPillText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RecentIncomeSection(
    entries: List<IncomeEntry>,
    currency: String,
    onEdit: (IncomeEntry) -> Unit,
    onDelete: (IncomeEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "RECENT INCOME",
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
                    elevation = IncomeTheme.ListElevation,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(IncomeTheme.ListGroupBackground)
                .border(1.dp, IncomeTheme.ListBorderColor, RoundedCornerShape(16.dp)),
        ) {
            entries.forEachIndexed { index, entry ->
                IncomeRowContent(
                    entry = entry,
                    currency = currency,
                    isRecurring = entry.recurringIncomeId != null,
                    onEdit = { onEdit(entry) },
                    onDelete = { onDelete(entry) },
                )
                if (index < entries.lastIndex) {
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
private fun IncomeRowContent(
    entry: IncomeEntry,
    currency: String,
    isRecurring: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val title = entry.note?.takeIf { it.isNotBlank() } ?: entry.accountName ?: "Income"

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
            IncomeIconView(
                label = title,
                chipBaseSurface = IncomeTheme.ListGroupBackground,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isRecurring) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Recurring income",
                            tint = AppColors.Muted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text(
                    buildString {
                        append(DateUtils.formatShortDate(entry.date))
                        entry.accountName?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                CurrencyFormatter.formatSigned(entry.amount, currency),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.Positive,
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Income options")
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