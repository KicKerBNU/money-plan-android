package com.moneyplann.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** Cross-platform icon keys (SF Symbol names from iOS) mapped to Material icons. */
object CategoryIcons {
    val pickerKeys: List<String> = listOf(
        "house.fill",
        "fork.knife",
        "person.3.fill",
        "pawprint.fill",
        "bus.fill",
        "paintpalette.fill",
        "paintbrush.fill",
        "tshirt.fill",
        "sparkles",
        "heart.fill",
        "graduationcap.fill",
        "gift.fill",
        "banknote.fill",
        "briefcase.fill",
        "laptopcomputer",
        "car.fill",
        "airplane",
        "cart.fill",
        "cup.and.saucer.fill",
        "dumbbell.fill",
        "book.fill",
        "wifi",
        "tv.fill",
        "wallet.pass.fill",
        "creditcard.fill",
        "building.columns.fill",
        "doc.text.fill",
        "dollarsign.circle.fill",
        "percent",
        "ellipsis",
    )

    val defaultPickerKey: String = pickerKeys.first()

    fun imageVector(iconKey: String?): ImageVector {
        if (iconKey.isNullOrBlank()) return Icons.Default.Category
        return iconMap[iconKey] ?: Icons.Default.Category
    }

    fun resolveIconKey(categoryName: String, storedIcon: String? = null): String {
        if (!storedIcon.isNullOrBlank()) return storedIcon
        val key = categoryName.trim().lowercase()
        return nameFallbackKeys[key] ?: defaultPickerKey
    }

    fun imageVectorForCategory(categoryName: String, storedIcon: String? = null): ImageVector =
        imageVector(resolveIconKey(categoryName, storedIcon))

    private val nameFallbackKeys = mapOf(
        "rent" to "house.fill",
        "pension" to "banknote.fill",
        "food" to "fork.knife",
        "social life" to "person.3.fill",
        "pets" to "pawprint.fill",
        "transport" to "bus.fill",
        "culture" to "paintpalette.fill",
        "household" to "paintbrush.fill",
        "apparel" to "tshirt.fill",
        "beauty" to "sparkles",
        "health" to "heart.fill",
        "education" to "graduationcap.fill",
        "gift" to "gift.fill",
        "other" to "ellipsis",
    )

    private val iconMap: Map<String, ImageVector> = mapOf(
        "house.fill" to Icons.Default.Home,
        "fork.knife" to Icons.Default.Restaurant,
        "person.3.fill" to Icons.Default.Groups,
        "pawprint.fill" to Icons.Default.Pets,
        "bus.fill" to Icons.Default.DirectionsCar,
        "paintpalette.fill" to Icons.Default.Palette,
        "paintbrush.fill" to Icons.Default.Brush,
        "tshirt.fill" to Icons.Default.Checkroom,
        "sparkles" to Icons.Default.AutoAwesome,
        "heart.fill" to Icons.Default.Favorite,
        "graduationcap.fill" to Icons.Default.School,
        "gift.fill" to Icons.Default.CardGiftcard,
        "banknote.fill" to Icons.Default.Payments,
        "briefcase.fill" to Icons.Default.Work,
        "laptopcomputer" to Icons.Default.Computer,
        "car.fill" to Icons.Default.DirectionsCar,
        "airplane" to Icons.Default.Flight,
        "cart.fill" to Icons.Default.ShoppingCart,
        "cup.and.saucer.fill" to Icons.Default.LocalCafe,
        "dumbbell.fill" to Icons.Default.FitnessCenter,
        "book.fill" to Icons.Default.MenuBook,
        "wifi" to Icons.Default.Wifi,
        "tv.fill" to Icons.Default.Tv,
        "wallet.pass.fill" to Icons.Default.AccountBalanceWallet,
        "creditcard.fill" to Icons.Default.CreditCard,
        "building.columns.fill" to Icons.Default.AccountBalance,
        "doc.text.fill" to Icons.Default.Description,
        "dollarsign.circle.fill" to Icons.Default.AttachMoney,
        "percent" to Icons.Default.Percent,
        "ellipsis" to Icons.Default.MoreHoriz,
    )
}

/** @deprecated Use [CategoryIcons.imageVectorForCategory] */
fun categoryIconForName(categoryName: String): ImageVector =
    CategoryIcons.imageVectorForCategory(categoryName)
