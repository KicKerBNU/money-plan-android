package com.moneyplann.app.ui.auth

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.moneyplann.app.AppContainer
import com.moneyplann.app.R
import com.moneyplann.app.ui.theme.AppColors
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("MONEY PLAN", style = MaterialTheme.typography.labelLarge, color = AppColors.Muted)
        Text(
            if (mode == AuthMode.SIGN_IN) "Sign in to your account" else "Create your account",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            "Use the same sign-in method as web or iOS: Google, or email/password if you created the account that way.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.Muted,
            textAlign = TextAlign.Center,
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = mode == AuthMode.SIGN_IN,
                onClick = { mode = AuthMode.SIGN_IN; errorMessage = ""; password = "" },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Sign in") }
            SegmentedButton(
                selected = mode == AuthMode.SIGN_UP,
                onClick = { mode = AuthMode.SIGN_UP; errorMessage = ""; password = "" },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Sign up") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                )
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = AppColors.Danger, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (mode == AuthMode.SIGN_IN) {
            TextButton(onClick = { showResetSheet = true }, modifier = Modifier.align(Alignment.End)) {
                Text("Forgot password?")
            }
        }

        Button(
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
            enabled = !isSubmitting && email.isNotBlank() && password.length >= 6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (mode == AuthMode.SIGN_IN) "Sign in with email" else "Create account")
        }

        HorizontalDivider()
        Text("OR", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)

        OutlinedButton(
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
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue with Google")
        }

        Spacer(Modifier.height(8.dp))
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset password") },
                navigationIcon = { TextButton(onClick = onDismiss) { Text("Cancel") } },
                actions = {
                    TextButton(onClick = { onSubmit(email.trim()) }, enabled = email.isNotBlank()) {
                        Text("Send")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Enter your email and we will send a reset link.", color = AppColors.Muted)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
        }
    }
}
