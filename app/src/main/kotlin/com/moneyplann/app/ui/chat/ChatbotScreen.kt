package com.moneyplann.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyplann.app.AppContainer
import com.moneyplann.app.data.models.ChatMessage
import com.moneyplann.app.data.models.ChatRole
import com.moneyplann.app.ui.components.ScreenHeader
import com.moneyplann.app.ui.settings.SettingsMenu
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.ChatTheme
import com.moneyplann.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val suggestedPrompts = listOf(
    "Where did I overspend?",
    "How much did I save this month?",
    "Show recurring expenses",
)

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

    fun sendPrompt(prompt: String) = send(textOverride = prompt)

    fun send(textOverride: String? = null) {
        val text = (textOverride ?: _state.value.input).trim()
        if (text.isEmpty() || text.length > 6000 || _state.value.isSending) return
        viewModelScope.launch {
            val userMessage = ChatMessage(role = ChatRole.USER, content = text)
            _state.update {
                it.copy(messages = it.messages + userMessage, input = "", isSending = true, errorMessage = null)
            }
            try {
                val reply = api.sendChat(_state.value.messages, DateUtils.localIsoDate())
                _state.update {
                    it.copy(
                        messages = it.messages + ChatMessage(role = ChatRole.ASSISTANT, content = reply),
                        isSending = false,
                    )
                }
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
    val screenBackground = ChatTheme.ScreenBackground
    val showSuggestedPrompts = hasConsent &&
        !state.isSending &&
        (state.messages.isEmpty() || state.messages.lastOrNull()?.role == ChatRole.ASSISTANT)

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        hasConsent = consentStore.hasConsent(uid)
        if (!hasConsent) showConsentSheet = true
    }

    LaunchedEffect(state.messages.size, state.isSending) {
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
        modifier = modifier.fillMaxSize(),
        containerColor = screenBackground,
        topBar = {
            ScreenHeader(
                title = "Expense assistant",
                subtitle = "Ask questions about your spending.",
                backgroundColor = screenBackground,
                actions = {
                    TextButton(
                        onClick = { viewModel.clear() },
                        enabled = state.messages.isNotEmpty() && hasConsent && !state.isSending,
                    ) {
                        Text("Clear", color = AppColors.ActionBlue)
                    }
                    SettingsMenu()
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!hasConsent) {
                    item {
                        ExpenseChatConsentBanner(onReview = { showConsentSheet = true })
                    }
                }

                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(message)
                }

                if (state.isSending) {
                    item { AssistantLoadingBubble(viewModel.loadingLabel) }
                }

                if (showSuggestedPrompts) {
                    item {
                        SuggestedPrompts(
                            prompts = suggestedPrompts,
                            onPromptClick = { viewModel.sendPrompt(it) },
                        )
                    }
                }

                state.errorMessage?.let { error ->
                    item {
                        Text(error, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (hasConsent) {
                ChatInputBar(
                    value = state.input,
                    onValueChange = viewModel::setInput,
                    onSend = { viewModel.send() },
                    isSending = state.isSending,
                    enabled = hasConsent,
                )
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
    if (isUser) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = message.content,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ChatTheme.UserBubbleBackground)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ChatTheme.AssistantIconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ChatTheme.AssistantIconTint,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = message.content,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ChatTheme.AssistantBubbleBackground)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AssistantLoadingBubble(label: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(ChatTheme.AssistantIconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = ChatTheme.AssistantIconTint,
                modifier = Modifier.size(14.dp),
            )
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(ChatTheme.AssistantBubbleBackground)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = ChatTheme.AssistantIconTint,
            )
            Text("$label…", color = AppColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SuggestedPrompts(
    prompts: List<String>,
    onPromptClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        prompts.forEach { prompt ->
            Text(
                text = prompt,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, ChatTheme.PromptBorder, RoundedCornerShape(20.dp))
                    .clickable { onPromptClick(prompt) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    enabled: Boolean,
) {
    val canSend = value.isNotBlank() && !isSending && enabled

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(ChatTheme.InputBackground)
            .border(1.dp, ChatTheme.InputBorder, RoundedCornerShape(28.dp))
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled && !isSending,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(AppColors.ActionBlue),
            maxLines = 6,
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            "Ask about your expenses",
                            color = AppColors.Muted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    inner()
                }
            },
        )
        IconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (canSend) ChatTheme.SendReadyBackground else ChatTheme.SendIdleBackground),
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun ExpenseChatConsentBanner(onReview: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ChatTheme.AssistantBubbleBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("AI consent required", fontWeight = FontWeight.SemiBold)
        Text(
            "This assistant sends your messages to our AI service. Review and accept to continue.",
            color = AppColors.Muted,
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onReview) {
            Text("Review consent", color = AppColors.ActionBlue)
        }
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
