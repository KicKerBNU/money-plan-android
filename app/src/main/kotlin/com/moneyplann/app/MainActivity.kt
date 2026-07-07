package com.moneyplann.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import com.moneyplann.app.core.AnalyticsHelper
import com.moneyplann.app.ui.auth.LoginScreen
import com.moneyplann.app.ui.components.LoadingStateView
import com.moneyplann.app.ui.components.ToastHost
import com.moneyplann.app.ui.navigation.AppNavigation
import com.moneyplann.app.ui.shared.FirebaseSetupScreen
import com.moneyplann.app.ui.theme.MoneyPlanTheme
import com.moneyplann.app.util.ToastManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themePrefs = AppContainer.themePreferences
            val preference by themePrefs.preference.collectAsState()
            val darkTheme = when (preference) {
                com.moneyplann.app.data.prefs.ThemePreference.SYSTEM -> isSystemInDarkTheme()
                com.moneyplann.app.data.prefs.ThemePreference.LIGHT -> false
                com.moneyplann.app.data.prefs.ThemePreference.DARK -> true
            }
            MoneyPlanTheme(darkTheme = darkTheme) {
                MoneyPlanRoot(activity = this)
            }
        }
    }
}

@Composable
fun MoneyPlanRoot(activity: ComponentActivity) {
    val auth = AppContainer.authRepository
    val currentUser by auth.currentUser.collectAsState()
    val isReady by auth.isReady.collectAsState()
    val sessionReady by auth.sessionReady.collectAsState()
    val firebaseConfigured = remember { isFirebaseConfigured() }

    LaunchedEffect(isReady, currentUser) {
        if (isReady && currentUser == null && firebaseConfigured) {
            AnalyticsHelper.logScreen("login")
        }
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null && !sessionReady) {
            auth.ensureSessionReady()
        }
    }

    LaunchedEffect(sessionReady, currentUser?.uid) {
        if (currentUser != null && sessionReady) {
            ToastManager.clear()
        }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            !firebaseConfigured -> FirebaseSetupScreen()
            !isReady -> LoadingStateView()
            currentUser == null -> LoginScreen(activity = activity)
            !sessionReady -> LoadingStateView()
            else -> AppNavigation()
        }
        ToastHost(Modifier.align(Alignment.TopCenter))
    }
}

private fun isFirebaseConfigured(): Boolean {
    return try {
        val appId = FirebaseApp.getInstance().options.applicationId
        appId.contains(":android:") && !appId.contains("replace_with", ignoreCase = true)
    } catch (_: Exception) {
        false
    }
}
