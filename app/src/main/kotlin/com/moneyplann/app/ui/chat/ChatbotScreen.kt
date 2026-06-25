package com.moneyplann.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.ChatMessage
import com.moneyplann.app.data.models.ChatRole
import com.moneyplann.app.ui.components.FinanceCard
import com.moneyplann.app.ui.settings.SettingsMenu
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

class ChatbotViewModel : ViewModel() {
    private val api = AppContainer.financeApi
    private val loadingVerbs = listOf("Analyzing", "Crunching", "Summing", "Comparing", "Scanning")
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    val loadingLabel: String get() = loadingVerbs.random()

    fun setInput(value: String) = _state.update { it.copy(input = value) }

    fun clear() = _state.update { it.copy(messages = emptyList(), errorMessage = null) }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || text.length > 6000) return
        viewModelScope.launch {
            val userMessage = ChatMessage(role = ChatRole.USER, content = text)
            _state.update { it.copy(messages = it.messages + userMessage, input = "", isSending = true, errorMessage = null) }
            try {
                val reply = api.sendChat(_state.value.messages, DateUtils.localIsoDate())
                _state.update { it.copy(messages = it.messages + ChatMessage(role = ChatRole.ASSISTANT, content = reply), isSending = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        messages = it.messages.filterNot { m -> m.id == userMessage.id },
                        isSending = false,
                        errorMessage = e.message,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(modifier: Modifier = Modifier, viewModel: ChatbotViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val auth = AppContainer.authRepository
    val currentUser by auth.currentUser.collectAsState()
    val consentStore = AppContainer.expenseChatConsentStore
    var hasConsent by remember { mutableStateOf(false) }
    var showConsentSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        hasConsent = consentStore.hasConsent(uid)
        if (!hasConsent) showConsentSheet = true
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    if (showConsentSheet && !hasConsent) {
        ExpenseChatConsentSheet(
            onAccept = {
                scope.launch {
                    val uid = currentUser?.uid ?: return@launch
                    consentStore.grantConsent(uid)
                    hasConsent = true
                    showConsentSheet = false
                }
            },
            onDismiss = { showConsentSheet = false },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Expense assistant") },
                actions = {
                    TextButton(onClick = { viewModel.clear() }, enabled = state.messages.isNotEmpty() && hasConsent) {
                        Text("Clear")
                    }
                    SettingsMenu()
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("Ask questions about your spending.", color = AppColors.Muted)
                }
                if (!hasConsent) {
                    item {
                        ExpenseChatConsentBanner(onReview = { showConsentSheet = true })
                    }
                } else if (state.messages.isEmpty()) {
                    item {
                        FinanceCard {
                            Text("Start a conversation about your expenses.", color = AppColors.Muted)
                        }
                    }
                }
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(message)
                }
                if (state.isSending) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                            Text("${viewModel.loadingLabel}…", color = AppColors.Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                state.errorMessage?.let { error ->
                    item { Text(error, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall) }
                }
            }
            if (hasConsent) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = state.input,
                        onValueChange = viewModel::setInput,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about your expenses") },
                        maxLines = 6,
                    )
                    IconButton(onClick = { viewModel.send() }, enabled = !state.isSending && state.input.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            } else {
                Text(
                    "Accept AI consent to use the assistant.",
                    modifier = Modifier.padding(16.dp),
                    color = AppColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    Box(Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = if (isUser) AppColors.Primary else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Text(
                message.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun ExpenseChatConsentBanner(onReview: () -> Unit) {
    FinanceCard {
        Text("AI consent required", fontWeight = FontWeight.SemiBold)
        Text("This assistant sends your messages to our AI service. Review and accept to continue.", color = AppColors.Muted)
        Button(onClick = onReview) { Text("Review consent") }
    }
}

@Composable
fun ExpenseChatConsentSheet(onAccept: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI expense assistant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Messages you send are processed by our AI provider to help answer expense questions.")
                Text("Read our privacy policy at moneyplann.com/privacy.")
            }
        },
        confirmButton = { TextButton(onClick = onAccept) { Text("I agree") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
