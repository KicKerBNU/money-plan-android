package com.moneyplann.app.data.auth

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.moneyplann.app.AppContainer
import com.moneyplann.app.R
import com.moneyplann.app.data.api.FinanceApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _sessionReady = MutableStateFlow(auth.currentUser == null)
    val sessionReady: StateFlow<Boolean> = _sessionReady.asStateFlow()

    val isAuthenticated: Boolean get() = activeUser() != null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            _isReady.value = true
            if (firebaseAuth.currentUser == null) {
                _sessionReady.value = true
            }
        }
        if (auth.currentUser != null) {
            _isReady.value = true
        } else {
            _sessionReady.value = true
        }
    }
    /** Prefer Firebase's current user — StateFlow can lag briefly after sign-in. */
    fun activeUser(): FirebaseUser? = auth.currentUser ?: _currentUser.value

    fun markSessionPending() {
        _sessionReady.value = false
    }

    private fun syncUser() {
        _currentUser.value = auth.currentUser
        _isReady.value = true
    }

    /** Prefetch a fresh ID token so the first API call after sign-in does not race auth. */
    suspend fun ensureSessionReady() {
        val user = activeUser() ?: run {
            _sessionReady.value = false
            return
        }
        idToken(forceRefresh = true)
        syncUser()
        _sessionReady.value = true
    }

    suspend fun idToken(forceRefresh: Boolean = false): String {
        val user = activeUser() ?: throw AuthException("Not authenticated")
        return user.getIdToken(forceRefresh).await().token
            ?: throw AuthException("Missing Firebase ID token")
    }

    suspend fun signIn(email: String, password: String) {
        _sessionReady.value = false
        try {
            auth.signInWithEmailAndPassword(normalizeEmail(email), password).await()
            ensureSessionReady()
        } catch (e: Exception) {
            AuthDebug.logFailure("signInWithEmailAndPassword", e)
            if (activeUser() == null) _sessionReady.value = true
            throw e
        }
    }

    suspend fun createAccount(email: String, password: String) {
        _sessionReady.value = false
        try {
            auth.createUserWithEmailAndPassword(normalizeEmail(email), password).await()
            ensureSessionReady()
        } catch (e: Exception) {
            AuthDebug.logFailure("createUserWithEmailAndPassword", e)
            if (activeUser() == null) _sessionReady.value = true
            throw e
        }
    }

    suspend fun sendPasswordReset(email: String) {
        try {
            auth.sendPasswordResetEmail(normalizeEmail(email)).await()
        } catch (e: Exception) {
            AuthDebug.logFailure("sendPasswordResetEmail", e)
            throw e
        }
    }

    suspend fun signInWithGoogle(activity: Activity) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId())
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        val account = GoogleSignIn.getLastSignedInAccount(activity)
            ?: googleSignInClient.signInIntent.let {
                throw GoogleSignInRequiredException(it)
            }
        val idToken = account.idToken ?: throw AuthException("Missing Google token")
        _sessionReady.value = false
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            ensureSessionReady()
        } catch (e: Exception) {
            AuthDebug.logFailure("signInWithGoogleCredential", e)
            if (activeUser() == null) _sessionReady.value = true
            throw e
        }
    }

    suspend fun signInWithGoogleResult(idToken: String) {
        _sessionReady.value = false
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            ensureSessionReady()
        } catch (e: Exception) {
            AuthDebug.logFailure("signInWithGoogleResult", e)
            if (activeUser() == null) _sessionReady.value = true
            throw e
        }
    }

    fun signOut() {
        auth.signOut()
        GoogleSignIn.getClient(
            auth.app.applicationContext,
            GoogleSignInOptions.DEFAULT_SIGN_IN,
        ).signOut()
        syncUser()
        _sessionReady.value = true
    }

    suspend fun deleteAccount(financeApi: FinanceApi) {
        financeApi.deleteCurrentUser()
        signOut()
    }

    fun friendlyError(error: Exception): String {
        if (error is ApiException && error.statusCode == 10) {
            return googleDeveloperErrorMessage()
        }
        val base = friendlyMessage(error)
        val detail = AuthDebug.technicalDetail(error)
        return if (detail != null) "$base\n$detail" else base
    }

    private fun friendlyMessage(error: Exception): String {
        if (error is FirebaseAuthException) {
            return when (error.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Enter a valid email address."
                "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_USER_MISMATCH" ->
                    invalidCredentialMessage()
                "ERROR_USER_NOT_FOUND" -> invalidCredentialMessage()
                "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                    "This email is already registered."
                "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
                "ERROR_USER_DISABLED" -> "This account has been disabled."
                "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a moment and try again."
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Email/password sign-in is disabled in Firebase. Enable it in Authentication → Sign-in method."
                else -> "Could not authenticate. Please try again."
            }
        }
        if (error is FirebaseNetworkException) {
            return "Network error. Check the emulator internet connection and try again."
        }
        val message = error.message.orEmpty().lowercase()
        return when {
            message.contains("invalid-credential") || message.contains("wrong-password") ->
                invalidCredentialMessage()
            message.contains("email-already-in-use") ->
                "This email is already registered."
            message.contains("weak-password") ->
                "Password must be at least 6 characters."
            message.contains("invalid-email") ->
                "Enter a valid email address."
            message.contains("network") ->
                "Network error. Check your connection and try again."
            else -> "Could not authenticate. Please try again."
        }
    }

    private fun invalidCredentialMessage(): String =
        "Email or password did not work. If you sign in on web or iOS with Google, use Continue with Google here. " +
            "Apple Sign-In accounts need a password — use Forgot password to set one, then try email again."

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    private fun webClientId(): String =
        AppContainer.appContext.getString(R.string.default_web_client_id)

    private fun googleDeveloperErrorMessage(): String =
        "Google Sign-In is not configured for this build. In Firebase Console → Project settings → " +
            "Android app com.moneyplann.app, add debug SHA-1 " +
            "25:2D:71:5F:B1:63:09:FB:01:58:94:F0:34:78:D1:1A:48:B0:36:8B, " +
            "then download a new google-services.json and rebuild."
}

class GoogleSignInRequiredException(val signInIntent: android.content.Intent) : Exception()
class AuthException(message: String) : Exception(message)
