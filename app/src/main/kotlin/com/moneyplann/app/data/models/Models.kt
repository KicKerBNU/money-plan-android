package com.moneyplann.app.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Account(
    val id: Int,
    val name: String,
    val isDefault: Boolean,
    val initialBalance: Double,
    val currentBalance: Double,
)

@Serializable
data class Category(
    val id: Int,
    val name: String,
    val isDefault: Boolean,
)

@Serializable
data class Expense(
    val id: Int,
    val date: String,
    val amount: Double,
    val categoryId: Int,
    val categoryName: String,
    val accountId: Int,
    val accountName: String,
    val note: String? = null,
    val manualSort: Int? = null,
)

@Serializable
data class IncomeEntry(
    val id: Int,
    val date: String,
    val amount: Double,
    val accountId: Int? = null,
    val accountName: String? = null,
    val note: String? = null,
)

@Serializable
data class MonthlyCategoryTotal(
    val categoryId: Int,
    val categoryName: String,
    val totalAmount: Double,
    val entryCount: Int,
)

@Serializable
data class MonthlyExpensesStats(
    val period: Period,
    val total: Double,
    val categories: List<MonthlyCategoryTotal>,
) {
    @Serializable
    data class Period(val year: Int, val month: Int)
}

enum class ChatRole {
    USER,
    ASSISTANT,
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
)

@Serializable
data class ExpenseChatRequestMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ExpenseChatReply(val reply: String)

@Serializable
data class DataResponse<T>(val data: T)

@Serializable
data class ApiErrorBody(val message: String? = null)

@Serializable
class EmptyResponse
