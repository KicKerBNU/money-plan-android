package com.moneyplann.app.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.prefs.ThemePreference
import kotlinx.coroutines.launch

@Composable
fun SettingsMenu(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val themePrefs = AppContainer.themePreferences
    val moneyPrefs = AppContainer.moneyPreferences
    val theme by themePrefs.preference.collectAsState()
    val currency by moneyPrefs.currency.collectAsState()

    IconButton(onClick = { expanded = true }, modifier = modifier) {
        Icon(Icons.Default.Settings, contentDescription = "Settings")
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Theme: ${theme.name.lowercase().replaceFirstChar { it.titlecase() }}") },
            onClick = {
                scope.launch {
                    val next = when (theme) {
                        ThemePreference.SYSTEM -> ThemePreference.LIGHT
                        ThemePreference.LIGHT -> ThemePreference.DARK
                        ThemePreference.DARK -> ThemePreference.SYSTEM
                    }
                    themePrefs.setPreference(next)
                }
            },
        )
        DropdownMenuItem(
            text = { Text("Currency: ${currency ?: "Auto (${moneyPrefs.autoCurrency})"}") },
            onClick = {
                scope.launch {
                    val current = currency ?: moneyPrefs.autoCurrency
                    val index = moneyPrefs.supportedCurrencies.indexOf(current)
                    val next = if (index < 0) null else {
                        val nextIndex = (index + 1) % (moneyPrefs.supportedCurrencies.size + 1)
                        if (nextIndex == moneyPrefs.supportedCurrencies.size) null
                        else moneyPrefs.supportedCurrencies[nextIndex]
                    }
                    moneyPrefs.setCurrency(next)
                }
            },
        )
        DropdownMenuItem(
            text = { Text("Delete account") },
            onClick = { expanded = false; showDeleteConfirm = true },
            enabled = !isDeleting,
        )
        DropdownMenuItem(
            text = { Text("Log out") },
            onClick = {
                expanded = false
                AppContainer.authRepository.signOut()
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete your account?") },
            text = {
                Text("This permanently deletes your Money Plan account, all expenses, income, accounts, and categories. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            try {
                                AppContainer.authRepository.deleteAccount(AppContainer.financeApi)
                            } finally {
                                isDeleting = false
                                showDeleteConfirm = false
                            }
                        }
                    },
                ) { Text("Delete account") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
