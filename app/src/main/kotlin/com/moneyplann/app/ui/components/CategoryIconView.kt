package com.moneyplann.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.CategoryColors

enum class CategoryIconShape {
    Circle,
    RoundedSquare,
}

@Composable
fun AccentIconChip(
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp,
    contentDescription: String? = null,
    chipBaseSurface: Color = AppColors.Surface,
    shape: CategoryIconShape = CategoryIconShape.Circle,
) {
    val chipShape: Shape = when (shape) {
        CategoryIconShape.Circle -> CircleShape
        CategoryIconShape.RoundedSquare -> RoundedCornerShape(14.dp)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(chipShape)
            .background(AppColors.iconChipBackground(accentColor, chipBaseSurface)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = AppColors.iconChipForeground(accentColor),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun CategoryIconView(
    categoryName: String,
    iconKey: String? = null,
    accentColor: Color = CategoryColors.accentForCategory(categoryName),
    modifier: Modifier = Modifier,
    chipBaseSurface: Color = AppColors.Surface,
    size: Dp = 36.dp,
    iconSize: Dp = 18.dp,
    shape: CategoryIconShape = CategoryIconShape.Circle,
) {
    AccentIconChip(
        icon = CategoryIcons.imageVectorForCategory(categoryName, iconKey),
        accentColor = accentColor,
        modifier = modifier,
        chipBaseSurface = chipBaseSurface,
        size = size,
        iconSize = iconSize,
        shape = shape,
    )
}
