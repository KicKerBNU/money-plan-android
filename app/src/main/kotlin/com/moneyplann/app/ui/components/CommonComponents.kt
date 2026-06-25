package com.moneyplann.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.util.ToastManager
import com.moneyplann.app.util.ToastType
import kotlinx.coroutines.delay

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
        delay(3000)
        ToastManager.clear()
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.padding(16.dp),
        snackbar = { snackbarData ->
            val type = toast?.type ?: ToastType.SUCCESS
            Snackbar(
                shape = RoundedCornerShape(12.dp),
                containerColor = when (type) {
                    ToastType.SUCCESS -> AppColors.Positive
                    ToastType.ERROR -> AppColors.Danger
                },
                contentColor = Color.White,
            ) {
                Text(snackbarData.visuals.message)
            }
        },
    )
}

@Composable
fun FinanceCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
fun KpiView(title: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(title, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
fun LoadingStateView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppColors.Primary)
    }
}

@Composable
fun EmptyStateView(
    title: String,
    message: String,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = AppColors.Muted)
        if (actionTitle != null && onAction != null) {
            Button(onClick = onAction) { Text(actionTitle) }
        }
    }
}

@Composable
fun ErrorStateView(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = AppColors.Danger, modifier = Modifier.padding(24.dp))
    }
}

@Composable
fun SummaryRow(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { (title, value) ->
            FinanceCard(Modifier.weight(1f)) {
                KpiView(title = title, value = value)
            }
        }
    }
}
