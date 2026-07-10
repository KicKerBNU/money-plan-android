package com.moneyplann.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.moneyplann.app.ui.theme.AppColors

enum class AccountVisualKind {
    Bank,
    Cash,
    Card,
    Other,
}

fun accountVisualKind(name: String): AccountVisualKind {
    val key = name.lowercase()
    return when {
        key.contains("cash") || key.contains("wallet") -> AccountVisualKind.Cash
        key.contains("card") || key.contains("credit") || key.contains("debit") -> AccountVisualKind.Card
        key.contains("bank") || key.contains("checking") || key.contains("savings") -> AccountVisualKind.Bank
        else -> AccountVisualKind.Other
    }
}

fun accountAccentColor(kind: AccountVisualKind): Color = when (kind) {
    AccountVisualKind.Bank -> Color(0xFF3B82F6)
    AccountVisualKind.Cash -> Color(0xFF22C55E)
    AccountVisualKind.Card -> Color(0xFF6366F1)
    AccountVisualKind.Other -> Color(0xFF3B82F6)
}

fun accountIconForKind(kind: AccountVisualKind): ImageVector = when (kind) {
    AccountVisualKind.Bank -> Icons.Default.AccountBalance
    AccountVisualKind.Cash -> Icons.Default.Payments
    AccountVisualKind.Card -> Icons.Default.CreditCard
    AccountVisualKind.Other -> Icons.Default.AccountBalance
}

@Composable
fun AccountIconView(
    accountName: String,
    modifier: Modifier = Modifier,
    chipBaseSurface: Color = AppColors.Surface,
) {
    val kind = accountVisualKind(accountName)
    val accent = accountAccentColor(kind)
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.iconChipBackground(accent, chipBaseSurface)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = accountIconForKind(kind),
            contentDescription = null,
            tint = AppColors.iconChipForeground(accent),
            modifier = Modifier.size(18.dp),
        )
    }
}
