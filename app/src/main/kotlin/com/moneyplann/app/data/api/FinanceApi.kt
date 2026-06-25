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
    ): Expense = client.fetchJson<DataResponse<Expense>>(
        path = "/v1/expenses",
        method = "POST",
        body = encode(CreateExpenseBody(date, amount, categoryId, accountId, note)),
    ).data

    suspend fun updateExpense(
        id: Int,
        date: String,
        amount: Double,
        categoryId: Int,
        accountId: Int,
        note: String?,
    ): Expense = client.fetchJson<DataResponse<Expense>>(
        path = "/v1/expenses/$id",
        method = "PUT",
        body = encode(CreateExpenseBody(date, amount, categoryId, accountId, note)),
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

    suspend fun createIncome(date: String, amount: Double, accountId: Int, note: String?): IncomeEntry =
        client.fetchJson<DataResponse<IncomeEntry>>(
            path = "/v1/incomes",
            method = "POST",
            body = encode(CreateIncomeBody(date, amount, accountId, note)),
        ).data

    suspend fun updateIncome(id: Int, date: String, amount: Double, accountId: Int, note: String?): IncomeEntry =
        client.fetchJson<DataResponse<IncomeEntry>>(
            path = "/v1/incomes/$id",
            method = "PUT",
            body = encode(CreateIncomeBody(date, amount, accountId, note)),
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
}

@Serializable
internal data class CreateExpenseBody(
    val date: String,
    val amount: Double,
    val categoryId: Int,
    val accountId: Int,
    val note: String? = null,
)

@Serializable internal data class ReorderExpensesBody(val year: Int, val month: Int, val orderedIds: List<Int>)
@Serializable internal data class NameBody(val name: String)
@Serializable internal data class EmptyBody(val placeholder: String? = null)
@Serializable internal data class CreateIncomeBody(val date: String, val amount: Double, val accountId: Int, val note: String? = null)
@Serializable internal data class ChatBody(val messages: List<ExpenseChatRequestMessage>, val clientToday: String)
