package com.moneyplann.app.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsMenu(modifier: Modifier = Modifier) {
    val openSettings = LocalOpenSettings.current
    IconButton(onClick = openSettings, modifier = modifier) {
        Icon(Icons.Default.Settings, contentDescription = "Settings")
    }
}
