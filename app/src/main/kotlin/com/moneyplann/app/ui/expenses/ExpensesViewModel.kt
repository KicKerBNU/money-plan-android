package com.moneyplann.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.data.models.Category
import com.moneyplann.app.data.models.Expense
import com.moneyplann.app.data.models.IncomeEntry
import com.moneyplann.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ExpensesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val expenses: List<Expense> = emptyList(),
    val incomes: List<IncomeEntry> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val searchText: String = "",
    val selectedCategoryIds: Set<Int> = emptySet(),
    val dateRangeStart: String? = null,
    val dateRangeEnd: String? = null,
) {
    val yearMonth = DateUtils.currentYearMonth()
    val year get() = yearMonth.first
    val month get() = yearMonth.second

    val canReorder: Boolean
        get() = searchText.isBlank() && selectedCategoryIds.isEmpty() && dateRangeStart == null

    val filteredExpenses: List<Expense>
        get() {
            var list = expenses
            if (searchText.isNotBlank()) {
                val q = searchText.lowercase()
                list = list.filter {
                    (it.note?.lowercase()?.contains(q) == true) ||
                        it.categoryName.lowercase().contains(q) ||
                        it.accountName.lowercase().contains(q) ||
                        it.date.contains(q)
                }
            }
            if (selectedCategoryIds.isNotEmpty()) {
                list = list.filter { selectedCategoryIds.contains(it.categoryId) }
            }
            if (dateRangeStart != null && dateRangeEnd != null) {
                list = list.filter { it.date >= dateRangeStart && it.date <= dateRangeEnd }
            }
            return list
        }

    val totalSpent: Double get() = filteredExpenses.sumOf { it.amount }
    val totalIncome: Double get() = incomes.sumOf { it.amount }
    val cashFlow: Double get() = totalIncome - totalSpent

    val categoryBreakdown: List<Pair<String, Double>>
        get() = expenses.groupBy { it.categoryName }
            .map { (name, items) -> name to items.sumOf { e -> e.amount } }
            .sortedWith { a, b ->
                when {
                    a.first.equals("other", true) -> 1
                    b.first.equals("other", true) -> -1
                    else -> b.second.compareTo(a.second)
                }
            }
}

class ExpensesViewModel : ViewModel() {
    private val api = AppContainer.financeApi
    private val _state = MutableStateFlow(ExpensesUiState())
    val state: StateFlow<ExpensesUiState> = _state.asStateFlow()

    fun setSearch(text: String) = _state.update { it.copy(searchText = text) }

    fun toggleCategory(id: Int) {
        _state.update { current ->
            val next = current.selectedCategoryIds.toMutableSet()
            if (next.contains(id)) next.remove(id) else next.add(id)
            current.copy(selectedCategoryIds = next)
        }
    }

    fun clearCategories() = _state.update { it.copy(selectedCategoryIds = emptySet()) }

    fun applyQuickRange(days: Int) {
        val end = LocalDate.now()
        val start = end.minusDays((days - 1).toLong())
        _state.update {
            it.copy(
                dateRangeStart = DateUtils.localIsoDate(start),
                dateRangeEnd = DateUtils.localIsoDate(end),
            )
        }
    }

    fun setDateRange(start: String, end: String) {
        _state.update { it.copy(dateRangeStart = start, dateRangeEnd = end) }
    }

    fun clearDateRange() = _state.update { it.copy(dateRangeStart = null, dateRangeEnd = null) }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val year = _state.value.year
                val month = _state.value.month
                val expenses = api.fetchExpenses(year, month)
                val incomes = api.fetchIncomes(year, month)
                val accounts = api.fetchAccounts()
                val categories = api.fetchCategories()
                _state.update {
                    it.copy(
                        isLoading = false,
                        expenses = expenses,
                        incomes = incomes,
                        accounts = accounts,
                        categories = categories,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun createExpense(date: String, amount: Double, categoryId: Int, accountId: Int, note: String?) {
        viewModelScope.launch {
            try {
                api.createExpense(date, amount, categoryId, accountId, note)
                load()
            } catch (_: Exception) {
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            val snapshot = _state.value.expenses
            _state.update { it.copy(expenses = it.expenses.filterNot { e -> e.id == expense.id }) }
            try {
                api.deleteExpense(expense.id)
            } catch (_: Exception) {
                _state.update { it.copy(expenses = snapshot) }
                load()
            }
        }
    }

    fun updateExpenseOptimistic(updated: Expense) {
        viewModelScope.launch {
            val index = _state.value.expenses.indexOfFirst { it.id == updated.id }
            if (index < 0) return@launch
            val snapshot = _state.value.expenses[index]
            _state.update { s ->
                s.copy(expenses = s.expenses.toMutableList().also { it[index] = updated })
            }
            try {
                val saved = api.updateExpense(
                    updated.id, updated.date, updated.amount,
                    updated.categoryId, updated.accountId, updated.note,
                )
                _state.update { s ->
                    s.copy(expenses = s.expenses.toMutableList().also { it[index] = saved })
                }
            } catch (_: Exception) {
                _state.update { s ->
                    s.copy(expenses = s.expenses.toMutableList().also { it[index] = snapshot })
                }
            }
        }
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        if (!_state.value.canReorder) return
        viewModelScope.launch {
            val ordered = _state.value.expenses.toMutableList()
            val item = ordered.removeAt(fromIndex)
            ordered.add(if (toIndex > fromIndex) toIndex - 1 else toIndex, item)
            val snapshot = _state.value.expenses
            _state.update { it.copy(expenses = ordered) }
            try {
                val saved = api.reorderExpenses(_state.value.year, _state.value.month, ordered.map { it.id })
                _state.update { it.copy(expenses = saved) }
            } catch (_: Exception) {
                _state.update { it.copy(expenses = snapshot) }
                load()
            }
        }
    }
}
