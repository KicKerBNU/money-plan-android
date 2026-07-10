package com.moneyplann.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneyplann.app.util.ToastManager
import com.moneyplann.app.util.ToastType
import kotlinx.coroutines.delay

/**
 * Material 3 snackbar host — bottom-aligned, errors only for routine CRUD.
 * Success for add/edit/delete is shown by the list updating (same as iOS).
 */
@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val toast by ToastManager.toast.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toast) {
        if (toast == null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }
        val current = toast ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current.message)
        delay(if (current.type == ToastType.ERROR) 5000 else 3500)
        ToastManager.clear()
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        snackbar = { snackbarData ->
            val type = toast?.type ?: ToastType.ERROR
            Snackbar(
                shape = RoundedCornerShape(8.dp),
                containerColor = if (type == ToastType.ERROR) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.inverseSurface
                },
                contentColor = if (type == ToastType.ERROR) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.inverseOnSurface
                },
            ) {
                Text(snackbarData.visuals.message)
            }
        },
    )
}
