package com.moneyplann.app.ui.auth

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.moneyplann.app.AppContainer
import com.moneyplann.app.R
import com.moneyplann.app.ui.components.GoogleSignInButton
import com.moneyplann.app.ui.theme.AppColors
import com.moneyplann.app.ui.theme.LoginTheme
import com.moneyplann.app.util.ToastManager
import kotlinx.coroutines.launch

private enum class AuthMode { SIGN_IN, SIGN_UP }

@Composable
fun LoginScreen(activity: Activity, modifier: Modifier = Modifier) {
    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showResetSheet by remember { mutableStateOf(false) }
    val auth = AppContainer.authRepository
    val authScope = (activity as ComponentActivity).lifecycleScope
    val scope = rememberCoroutineScope()
    val fieldColors = loginFieldColors()

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        authScope.launch {
            isSubmitting = true
            errorMessage = ""
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val token = account.idToken ?: throw IllegalStateException("Missing Google token")
                auth.signInWithGoogleResult(token)
                ToastManager.clear()
            } catch (e: Exception) {
                if (auth.isAuthenticated) {
                    ToastManager.clear()
                } else if (e !is ApiException || e.statusCode != 12501) {
                    errorMessage = auth.friendlyError(e)
                }
            } finally {
                isSubmitting = false
            }
        }
    }

    if (showResetSheet) {
        PasswordResetSheet(
            prefilledEmail = email,
            onDismiss = { showResetSheet = false },
            onSubmit = { resetEmail ->
                scope.launch {
                    try {
                        auth.sendPasswordReset(resetEmail)
                        ToastManager.success("Password reset email sent.")
                        showResetSheet = false
                    } catch (e: Exception) {
                        ToastManager.error(auth.friendlyError(e))
                    }
                }
            },
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = LoginTheme.ScreenBackground,
        contentColor = LoginTheme.PrimaryText,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "Money Plan logo",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop,
            )

            Text(
                "MONEY PLAN",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = LoginTheme.SecondaryText,
                letterSpacing = 1.2.sp,
            )

            Text(
                if (mode == AuthMode.SIGN_IN) "Welcome back" else "Create your account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = LoginTheme.PrimaryText,
                textAlign = TextAlign.Center,
            )

            Text(
                "Sign in with the same method you used on web or iOS.",
                style = MaterialTheme.typography.bodyMedium,
                color = LoginTheme.SecondaryText,
                textAlign = TextAlign.Center,
            )

            AuthModeTabs(
                selected = mode,
                onSelect = { selected ->
                    mode = selected
                    errorMessage = ""
                    password = ""
                },
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = LoginTheme.SecondaryText) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = fieldColors,
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = LoginTheme.SecondaryText) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = fieldColors,
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = LoginTheme.SecondaryText,
                        )
                    }
                },
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    errorMessage,
                    color = AppColors.Danger,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (mode == AuthMode.SIGN_IN) {
                TextButton(
                    onClick = { showResetSheet = true },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Forgot password?", color = AppColors.ActionBlue)
                }
            } else {
                Spacer(Modifier.height(4.dp))
            }

            LoginPrimaryButton(
                text = if (mode == AuthMode.SIGN_IN) "Sign in with email" else "Create account",
                enabled = !isSubmitting && email.isNotBlank() && password.length >= 6,
                loading = isSubmitting,
                onClick = {
                    authScope.launch {
                        isSubmitting = true
                        errorMessage = ""
                        try {
                            if (mode == AuthMode.SIGN_IN) auth.signIn(email, password)
                            else auth.createAccount(email, password)
                            ToastManager.clear()
                        } catch (e: Exception) {
                            errorMessage = auth.friendlyError(e)
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
            )

            OrDivider()

            GoogleSignInButton(
                enabled = !isSubmitting,
                onClick = {
                    authScope.launch {
                        errorMessage = ""
                        try {
                            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN,
                            )
                                .requestIdToken(activity.getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val client = GoogleSignIn.getClient(activity, gso)
                            isSubmitting = true
                            auth.markSessionPending()
                            googleLauncher.launch(client.signInIntent)
                        } catch (e: Exception) {
                            errorMessage = auth.friendlyError(e)
                            isSubmitting = false
                        }
                    }
                },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LoginTheme.PrimaryText,
    unfocusedTextColor = LoginTheme.PrimaryText,
    disabledTextColor = LoginTheme.PrimaryText.copy(alpha = 0.45f),
    focusedLabelColor = LoginTheme.SecondaryText,
    unfocusedLabelColor = LoginTheme.SecondaryText,
    cursorColor = AppColors.ActionBlue,
    focusedBorderColor = LoginTheme.TabBorder,
    unfocusedBorderColor = LoginTheme.TabBorder,
)

@Composable
private fun AuthModeTabs(selected: AuthMode, onSelect: (AuthMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, LoginTheme.TabBorder, RoundedCornerShape(12.dp)),
    ) {
        AuthMode.entries.forEach { mode ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isSelected) LoginTheme.TabSelected else LoginTheme.ScreenBackground)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (mode == AuthMode.SIGN_IN) "Sign in" else "Sign up",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = LoginTheme.PrimaryText,
                )
            }
        }
    }
}

@Composable
private fun LoginPrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) LoginTheme.PrimaryButton else LoginTheme.PrimaryButton.copy(alpha = 0.45f))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LoginTheme.PrimaryButtonText,
            )
        } else {
            Text(
                text,
                color = LoginTheme.PrimaryButtonText,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(Modifier.weight(1f), color = LoginTheme.Divider)
        Text("OR", style = MaterialTheme.typography.labelSmall, color = LoginTheme.SecondaryText)
        HorizontalDivider(Modifier.weight(1f), color = LoginTheme.Divider)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordResetSheet(
    prefilledEmail: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var email by remember(prefilledEmail) { mutableStateOf(prefilledEmail) }
    val fieldColors = loginFieldColors()

    Surface(color = LoginTheme.ScreenBackground, contentColor = LoginTheme.PrimaryText) {
        Scaffold(
            containerColor = LoginTheme.ScreenBackground,
            contentColor = LoginTheme.PrimaryText,
            topBar = {
                TopAppBar(
                    title = { Text("Reset password", color = LoginTheme.PrimaryText) },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = AppColors.ActionBlue)
                        }
                    },
                    actions = {
                        TextButton(onClick = { onSubmit(email.trim()) }, enabled = email.isNotBlank()) {
                            Text("Send", color = AppColors.ActionBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = LoginTheme.ScreenBackground,
                        titleContentColor = LoginTheme.PrimaryText,
                    ),
                )
            },
        ) { padding ->
            Column(Modifier.padding(padding).padding(16.dp)) {
                Text("Enter your email and we will send a reset link.", color = LoginTheme.SecondaryText)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = LoginTheme.SecondaryText) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = fieldColors,
                )
            }
        }
    }
}
