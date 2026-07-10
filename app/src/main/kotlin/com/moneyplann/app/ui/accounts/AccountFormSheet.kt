package com.moneyplann.app.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneyplann.app.data.models.Account
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.AccountsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormSheet(
    editing: Account? = null,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
    var errorMessage by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AccountsTheme.FormSheetBackground,
    ) {
        Scaffold(
            containerColor = AccountsTheme.FormSheetBackground,
            topBar = {
                TopAppBar(
                    title = { Text(if (editing == null) "New account" else "Edit account") },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = AppColors.ActionBlue)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            val trimmed = name.trim()
                            if (trimmed.isBlank()) {
                                errorMessage = "Enter an account name."
                                return@TextButton
                            }
                            onSave(trimmed)
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Account name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
                }

                if (editing != null && onDelete != null) {
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Delete account", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
