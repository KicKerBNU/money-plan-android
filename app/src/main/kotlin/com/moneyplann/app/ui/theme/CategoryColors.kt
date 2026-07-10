package com.moneyplann.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Shared category accent palette used across expenses, recurring, and lists. */
object CategoryColors {
    private val segments = listOf(
        Color(0xFF5B82E0), // Rent
        Color(0xFF57B06E), // Other
        Color(0xFFB8956A), // Transport
        Color(0xFFE05858), // Food
        Color(0xFF9333EA), // Pets
        Color(0xFF9CA3AF),
        Color(0xFFEA580C),
        Color(0xFF2F7F73),
    )

    fun accent(index: Int): Color = segments.getOrElse(index) { Color(0xFF9CA3AF) }

    fun accentForCategory(categoryName: String, breakdown: List<Pair<String, Double>>? = null): Color {
        breakdown?.let { list ->
            val index = list.indexOfFirst { it.first.equals(categoryName, ignoreCase = true) }
            if (index >= 0) return accent(index)
        }
        val key = categoryName.lowercase()
        val index = when {
            key.contains("rent") || key.contains("home") || key.contains("housing") -> 0
            key.contains("other") -> 1
            key.contains("transport") || key.contains("car") || key.contains("bus") -> 2
            key.contains("food") || key.contains("grocer") -> 3
            key.contains("pet") -> 4
            else -> categoryName.hashCode().mod(segments.size).let { if (it < 0) -it else it }
        }
        return accent(index)
    }
}
