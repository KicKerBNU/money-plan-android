package com.moneyplann.app.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.data.models.Category
import com.moneyplann.app.data.models.RecurrenceFrequency
import com.moneyplann.app.data.models.RecurringExpense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecurringExpensesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val items: List<RecurringExpense> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
)

class RecurringExpensesViewModel : ViewModel() {
    private val api = AppContainer.financeApi
    private val _state = MutableStateFlow(RecurringExpensesUiState())
    val state: StateFlow<RecurringExpensesUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val items = api.fetchRecurringExpenses()
                val accounts = api.fetchAccounts()
                val categories = api.fetchCategories()
                _state.update {
                    it.copy(isLoading = false, items = items, accounts = accounts, categories = categories)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun setActive(item: RecurringExpense, active: Boolean) {
        viewModelScope.launch {
            try {
                val saved = api.setRecurringExpenseActive(item.id, active)
                _state.update { current ->
                    current.copy(
                        items = current.items.map { if (it.id == saved.id) saved else it },
                    )
                }
            } catch (_: Exception) {
                load()
            }
        }
    }

    fun update(item: RecurringExpense) {
        viewModelScope.launch {
            try {
                val saved = api.updateRecurringExpense(
                    id = item.id,
                    amount = item.amount,
                    categoryId = item.categoryId,
                    accountId = item.accountId,
                    note = item.note,
                    frequency = item.frequency,
                    startDate = item.startDate,
                )
                val synced = if (item.active != saved.active) {
                    api.setRecurringExpenseActive(item.id, item.active)
                } else {
                    saved
                }
                _state.update { current ->
                    current.copy(
                        items = current.items.map { if (it.id == synced.id) synced else it },
                    )
                }
            } catch (_: Exception) {
                load()
            }
        }
    }

    fun delete(item: RecurringExpense) {
        viewModelScope.launch {
            val snapshot = _state.value.items
            _state.update { it.copy(items = it.items.filterNot { row -> row.id == item.id }) }
            try {
                api.deleteRecurringExpense(item.id)
            } catch (_: Exception) {
                _state.update { it.copy(items = snapshot) }
                load()
            }
        }
    }
}
