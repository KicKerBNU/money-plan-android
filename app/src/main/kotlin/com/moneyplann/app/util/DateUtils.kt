package com.moneyplann.app.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

object DateUtils {
    private val shortFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    fun localIsoDate(date: LocalDate = LocalDate.now()): String = date.toString()

    fun parseLocalIsoDate(iso: String): LocalDate? = runCatching { LocalDate.parse(iso.take(10)) }.getOrNull()

    /** Material3 DatePicker stores calendar dates as UTC midnight — not local timezone. */
    fun datePickerUtcMillis(date: LocalDate = LocalDate.now()): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun localDateFromDatePickerUtcMillis(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun currentYearMonth(): Pair<Int, Int> {
        val now = LocalDate.now()
        return now.year to now.monthValue
    }

    fun formatShortDate(iso: String): String {
        return parseLocalIsoDate(iso)?.format(shortFormatter) ?: iso
    }
}

object CurrencyFormatter {
    fun format(amount: Double, currencyCode: String): String {
        return runCatching {
            NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                currency = Currency.getInstance(currencyCode)
            }.format(amount)
        }.getOrDefault(amount.toString())
    }

    fun formatSigned(amount: Double, currencyCode: String): String {
        val prefix = if (amount >= 0) "+" else ""
        return prefix + format(amount, currencyCode)
    }
}

object DefaultAccountPicker {
    fun pick(accounts: List<com.moneyplann.app.data.models.Account>): com.moneyplann.app.data.models.Account? {
        if (accounts.isEmpty()) return null
        return accounts.firstOrNull { it.isDefault }
            ?: accounts.firstOrNull { it.name.contains("Bank", ignoreCase = true) }
            ?: accounts.first()
    }
}

object CategoryIcon {
    fun materialIcon(categoryName: String): String {
        val key = categoryName.lowercase(Locale.getDefault())
        return when {
            key.contains("food") || key.contains("grocer") -> "restaurant"
            key.contains("transport") || key.contains("car") -> "directions_car"
            key.contains("rent") || key.contains("home") || key.contains("housing") -> "home"
            key.contains("health") || key.contains("medical") -> "local_hospital"
            key.contains("entertain") -> "movie"
            key.contains("shop") -> "shopping_bag"
            key.contains("travel") -> "flight"
            key.contains("education") -> "school"
            key.contains("utility") || key.contains("bill") -> "receipt_long"
            key.contains("salary") || key.contains("income") -> "payments"
            else -> "category"
        }
    }
}
