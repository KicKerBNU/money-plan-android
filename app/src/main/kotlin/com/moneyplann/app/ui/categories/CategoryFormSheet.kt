package com.moneyplann.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneyplann.app.data.models.Category
import com.moneyplann.app.ui.components.CategoryIcons
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.CategoriesTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormSheet(
    editing: Category? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, iconKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var selectedIcon by remember(editing) {
        mutableStateOf(CategoryIcons.resolveIconKey(editing?.name.orEmpty(), editing?.icon))
    }
    var errorMessage by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = CategoriesTheme.FormSheetBackground,
    ) {
        Scaffold(
            containerColor = CategoriesTheme.FormSheetBackground,
            topBar = {
                TopAppBar(
                    title = { Text(if (editing == null) "New category" else "Edit category") },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = AppColors.ActionBlue)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            val trimmed = name.trim()
                            if (trimmed.isBlank()) {
                                errorMessage = "Enter a category name."
                                return@TextButton
                            }
                            onSave(trimmed, selectedIcon)
                        }) {
                            Text("Save", color = AppColors.ActionBlue)
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Text(
                    "ICON",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Muted,
                    letterSpacing = 0.6.sp,
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryIcons.pickerKeys.chunked(6).forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowKeys.forEach { iconKey ->
                                val selected = iconKey == selectedIcon
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) CategoriesTheme.IconPickerSelected
                                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        )
                                        .clickable { selectedIcon = iconKey },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = CategoryIcons.imageVector(iconKey),
                                        contentDescription = null,
                                        tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            repeat(6 - rowKeys.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
