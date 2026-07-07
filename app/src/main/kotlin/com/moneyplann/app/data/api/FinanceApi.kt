package com.moneyplann.app.data.api

import com.moneyplann.app.data.models.Account
import com.moneyplann.app.data.models.Category
import com.moneyplann.app.data.models.ChatMessage
import com.moneyplann.app.data.models.ChatRole
import com.moneyplann.app.data.models.DataResponse
import com.moneyplann.app.data.models.Expense
import com.moneyplann.app.data.models.ExpenseChatReply
import com.moneyplann.app.data.models.ExpenseChatRequestMessage
import com.moneyplann.app.data.models.IncomeEntry
import com.moneyplann.app.data.models.MonthlyExpensesStats
import com.moneyplann.app.data.models.RecurrenceFrequency
import com.moneyplann.app.data.models.RecurringExpense
import com.moneyplann.app.data.models.RecurringIncome
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FinanceApi(private val client: ApiClient) {
    private inline fun <reified T> encode(body: T): String = client.json.encodeToString(body)
    suspend fun fetchExpenses(year: Int, month: Int): List<Expense> =
        client.fetchJson<DataResponse<List<Expense>>>(
            "/v1/expenses?year=$year&month=$month",
            silentError = true,
        ).data

    suspend fun createExpense(
        date: String,
        amount: Double,
        categoryId: Int,
        accountId: Int,
        note: String?,
        recurrenceFrequency: RecurrenceFrequency? = null,
    ): Expense = client.fetchJson<DataResponse<Expense>>(
        path = "/v1/expenses",
        method = "POST",
        body = encode(
            CreateExpenseBody(
                date = date,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                recurrenceFrequency = recurrenceFrequency,
            ),
        ),
    ).data

    suspend fun updateExpense(
        id: Int,
        date: String,
        amount: Double,
        categoryId: Int,
        accountId: Int,
        note: String?,
        recurrenceEnabled: Boolean? = null,
        recurrenceFrequency: RecurrenceFrequency? = null,
    ): Expense = client.fetchJson<DataResponse<Expense>>(
        path = "/v1/expenses/$id",
        method = "PUT",
        body = encode(
            UpdateExpenseBody(
                date = date,
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                recurrenceEnabled = recurrenceEnabled,
                recurrenceFrequency = recurrenceFrequency,
            ),
        ),
    ).data

    suspend fun deleteExpense(id: Int) {
        client.fetchVoid("/v1/expenses/$id", method = "DELETE")
    }

    suspend fun reorderExpenses(year: Int, month: Int, orderedIds: List<Int>): List<Expense> =
        client.fetchJson<DataResponse<List<Expense>>>(
            path = "/v1/expenses/order",
            method = "PATCH",
            body = encode(ReorderExpensesBody(year, month, orderedIds)),
        ).data

    suspend fun fetchAccounts(): List<Account> =
        client.fetchJson<DataResponse<List<Account>>>("/v1/accounts", silentError = true).data

    suspend fun createAccount(name: String): Account =
        client.fetchJson<DataResponse<Account>>(
            path = "/v1/accounts",
            method = "POST",
            body = encode(NameBody(name)),
        ).data

    suspend fun updateAccount(id: Int, name: String): Account =
        client.fetchJson<DataResponse<Account>>(
            path = "/v1/accounts/$id",
            method = "PUT",
            body = encode(NameBody(name)),
        ).data

    suspend fun setAccountDefault(id: Int): Account =
        client.fetchJson<DataResponse<Account>>(
            path = "/v1/accounts/$id/default",
            method = "PATCH",
            body = encode(EmptyBody()),
        ).data

    suspend fun deleteAccount(id: Int) {
        client.fetchVoid("/v1/accounts/$id", method = "DELETE")
    }

    suspend fun fetchCategories(): List<Category> =
        client.fetchJson<DataResponse<List<Category>>>("/v1/categories", silentError = true).data

    suspend fun fetchIncomes(year: Int, month: Int): List<IncomeEntry> =
        client.fetchJson<DataResponse<List<IncomeEntry>>>(
            "/v1/incomes?year=$year&month=$month",
            silentError = true,
        ).data

    suspend fun createIncome(
        date: String,
        amount: Double,
        accountId: Int,
        note: String?,
        recurrenceFrequency: RecurrenceFrequency? = null,
    ): IncomeEntry =
        client.fetchJson<DataResponse<IncomeEntry>>(
            path = "/v1/incomes",
            method = "POST",
            body = encode(
                CreateIncomeBody(
                    date = date,
                    amount = amount,
                    accountId = accountId,
                    note = note,
                    recurrenceFrequency = recurrenceFrequency,
                ),
            ),
        ).data

    suspend fun updateIncome(
        id: Int,
        date: String,
        amount: Double,
        accountId: Int,
        note: String?,
        recurrenceEnabled: Boolean? = null,
        recurrenceFrequency: RecurrenceFrequency? = null,
    ): IncomeEntry =
        client.fetchJson<DataResponse<IncomeEntry>>(
            path = "/v1/incomes/$id",
            method = "PUT",
            body = encode(
                UpdateIncomeBody(
                    date = date,
                    amount = amount,
                    accountId = accountId,
                    note = note,
                    recurrenceEnabled = recurrenceEnabled,
                    recurrenceFrequency = recurrenceFrequency,
                ),
            ),
        ).data

    suspend fun deleteIncome(id: Int) {
        client.fetchVoid("/v1/incomes/$id", method = "DELETE")
    }

    suspend fun fetchMonthlyStats(year: Int, month: Int): MonthlyExpensesStats =
        client.fetchJson<DataResponse<MonthlyExpensesStats>>(
            "/v1/stats/monthly-expenses?year=$year&month=$month",
            silentError = true,
        ).data

    suspend fun sendChat(messages: List<ChatMessage>, clientToday: String): String {
        val payload = ChatBody(
            messages = messages.map {
                ExpenseChatRequestMessage(
                    role = if (it.role == ChatRole.USER) "user" else "assistant",
                    content = it.content,
                )
            },
            clientToday = clientToday,
        )
        return client.fetchJson<DataResponse<ExpenseChatReply>>(
            path = "/v1/ai/expense-chat",
            method = "POST",
            body = encode(payload),
            silentError = true,
            silentSuccess = true,
        ).data.reply
    }

    suspend fun deleteCurrentUser() {
        client.fetchVoid("/v1/me", method = "DELETE", successMessage = "Your account was deleted.")
    }

    suspend fun fetchRecurringExpenses(): List<RecurringExpense> =
        client.fetchJson<DataResponse<List<RecurringExpense>>>("/v1/recurring-expenses", silentError = true).data

    suspend fun fetchRecurringExpense(id: Int): RecurringExpense =
        client.fetchJson<DataResponse<RecurringExpense>>("/v1/recurring-expenses/$id").data

    suspend fun setRecurringExpenseActive(id: Int, active: Boolean): RecurringExpense =
        client.fetchJson<DataResponse<RecurringExpense>>(
            path = "/v1/recurring-expenses/$id",
            method = "PATCH",
            body = encode(RecurringActiveBody(active)),
        ).data

    suspend fun updateRecurringExpense(
        id: Int,
        amount: Double,
        categoryId: Int,
        accountId: Int,
        note: String?,
        frequency: RecurrenceFrequency,
        startDate: String,
    ): RecurringExpense = client.fetchJson<DataResponse<RecurringExpense>>(
        path = "/v1/recurring-expenses/$id",
        method = "PUT",
        body = encode(
            UpdateRecurringExpenseBody(
                amount = amount,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                frequency = frequency,
                startDate = startDate,
            ),
        ),
    ).data

    suspend fun deleteRecurringExpense(id: Int) {
        client.fetchVoid("/v1/recurring-expenses/$id", method = "DELETE")
    }

    suspend fun fetchRecurringIncomes(): List<RecurringIncome> =
        client.fetchJson<DataResponse<List<RecurringIncome>>>("/v1/recurring-incomes", silentError = true).data

    suspend fun fetchRecurringIncome(id: Int): RecurringIncome =
        client.fetchJson<DataResponse<RecurringIncome>>("/v1/recurring-incomes/$id").data

    suspend fun setRecurringIncomeActive(id: Int, active: Boolean): RecurringIncome =
        client.fetchJson<DataResponse<RecurringIncome>>(
            path = "/v1/recurring-incomes/$id",
            method = "PATCH",
            body = encode(RecurringActiveBody(active)),
        ).data

    suspend fun updateRecurringIncome(
        id: Int,
        amount: Double,
        accountId: Int,
        note: String?,
        frequency: RecurrenceFrequency,
        startDate: String,
    ): RecurringIncome = client.fetchJson<DataResponse<RecurringIncome>>(
        path = "/v1/recurring-incomes/$id",
        method = "PUT",
        body = encode(
            UpdateRecurringIncomeBody(
                amount = amount,
                accountId = accountId,
                note = note,
                frequency = frequency,
                startDate = startDate,
            ),
        ),
    ).data

    suspend fun deleteRecurringIncome(id: Int) {
        client.fetchVoid("/v1/recurring-incomes/$id", method = "DELETE")
    }
}

@Serializable
internal data class CreateExpenseBody(
    val date: String,
    val amount: Double,
    val categoryId: Int,
    val accountId: Int,
    val note: String? = null,
    val recurrenceFrequency: RecurrenceFrequency? = null,
)

@Serializable
internal data class UpdateExpenseBody(
    val date: String,
    val amount: Double,
    val categoryId: Int,
    val accountId: Int,
    val note: String? = null,
    val recurrenceEnabled: Boolean? = null,
    val recurrenceFrequency: RecurrenceFrequency? = null,
)

@Serializable internal data class RecurringActiveBody(val active: Boolean)

@Serializable
internal data class UpdateRecurringExpenseBody(
    val amount: Double,
    val categoryId: Int,
    val accountId: Int,
    val note: String? = null,
    val frequency: RecurrenceFrequency,
    val startDate: String,
)

@Serializable internal data class ReorderExpensesBody(val year: Int, val month: Int, val orderedIds: List<Int>)
@Serializable internal data class NameBody(val name: String)
@Serializable internal data class EmptyBody(val placeholder: String? = null)
@Serializable
internal data class CreateIncomeBody(
    val date: String,
    val amount: Double,
    val accountId: Int,
    val note: String? = null,
    val recurrenceFrequency: RecurrenceFrequency? = null,
)

@Serializable
internal data class UpdateIncomeBody(
    val date: String,
    val amount: Double,
    val accountId: Int,
    val note: String? = null,
    val recurrenceEnabled: Boolean? = null,
    val recurrenceFrequency: RecurrenceFrequency? = null,
)

@Serializable
internal data class UpdateRecurringIncomeBody(
    val amount: Double,
    val accountId: Int,
    val note: String? = null,
    val frequency: RecurrenceFrequency,
    val startDate: String,
)
@Serializable internal data class ChatBody(val messages: List<ExpenseChatRequestMessage>, val clientToday: String)
