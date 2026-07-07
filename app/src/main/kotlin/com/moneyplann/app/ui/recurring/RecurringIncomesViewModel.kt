package com.moneyplann.app.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.data.models.RecurrenceFrequency
import com.moneyplann.app.data.models.RecurringIncome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecurringIncomesUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val items: List<RecurringIncome> = emptyList(),
    val accounts: List<Account> = emptyList(),
)

class RecurringIncomesViewModel : ViewModel() {
    private val api = AppContainer.financeApi
    private val _state = MutableStateFlow(RecurringIncomesUiState())
    val state: StateFlow<RecurringIncomesUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val items = api.fetchRecurringIncomes()
                val accounts = api.fetchAccounts()
                _state.update {
                    it.copy(isLoading = false, items = items, accounts = accounts)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun setActive(item: RecurringIncome, active: Boolean) {
        viewModelScope.launch {
            try {
                val saved = api.setRecurringIncomeActive(item.id, active)
                _state.update { current ->
                    current.copy(items = current.items.map { if (it.id == saved.id) saved else it })
                }
            } catch (_: Exception) {
                load()
            }
        }
    }

    fun update(item: RecurringIncome) {
        viewModelScope.launch {
            try {
                val saved = api.updateRecurringIncome(
                    id = item.id,
                    amount = item.amount,
                    accountId = item.accountId,
                    note = item.note,
                    frequency = item.frequency,
                    startDate = item.startDate,
                )
                val synced = if (item.active != saved.active) {
                    api.setRecurringIncomeActive(item.id, item.active)
                } else {
                    saved
                }
                _state.update { current ->
                    current.copy(items = current.items.map { if (it.id == synced.id) synced else it })
                }
            } catch (_: Exception) {
                load()
            }
        }
    }

    fun delete(item: RecurringIncome) {
        viewModelScope.launch {
            val snapshot = _state.value.items
            _state.update { it.copy(items = it.items.filterNot { row -> row.id == item.id }) }
            try {
                api.deleteRecurringIncome(item.id)
            } catch (_: Exception) {
                _state.update { it.copy(items = snapshot) }
                load()
            }
        }
    }
}
