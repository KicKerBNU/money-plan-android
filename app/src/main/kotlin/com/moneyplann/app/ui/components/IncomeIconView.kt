package com.moneyplann.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.IncomeTheme

fun incomeIconForLabel(label: String): ImageVector {
    val key = label.lowercase()
    return when {
        key.contains("freelance") || key.contains("contract") || key.contains("consult") -> Icons.Default.Computer
        key.contains("salary") || key.contains("payroll") || key.contains("wage") -> Icons.Default.Work
        key.contains("rent") || key.contains("tenant") -> Icons.Default.Home
        else -> Icons.Default.Payments
    }
}

@Composable
fun IncomeIconView(
    label: String,
    accentColor: Color = IncomeTheme.IconAccent,
    modifier: Modifier = Modifier,
    chipBaseSurface: Color = IncomeTheme.ListGroupBackground,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.iconChipBackground(accentColor, chipBaseSurface)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = incomeIconForLabel(label),
            contentDescription = null,
            tint = AppColors.iconChipForeground(accentColor),
            modifier = Modifier.size(18.dp),
        )
    }
}
