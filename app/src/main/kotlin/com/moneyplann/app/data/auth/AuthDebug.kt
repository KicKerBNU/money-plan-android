package com.moneyplann.app.data.auth

import android.util.Log
import com.moneyplann.app.BuildConfig
import com.google.firebase.auth.FirebaseAuthException

object AuthDebug {
    private const val TAG = "MoneyPlanAuth"

    fun logFailure(action: String, error: Throwable) {
        if (!BuildConfig.DEBUG) return
        val firebase = error as? FirebaseAuthException
        if (firebase != null) {
            Log.e(
                TAG,
                "$action failed: errorCode=${firebase.errorCode}, message=${firebase.message}",
                error,
            )
        } else {
            Log.e(TAG, "$action failed: ${error.javaClass.simpleName}: ${error.message}", error)
        }
    }

    /** Full detail for debug builds — appended under the friendly message in the login form. */
    fun technicalDetail(error: Throwable): String? {
        if (!BuildConfig.DEBUG) return null
        val firebase = error as? FirebaseAuthException
        return if (firebase != null) {
            "[debug] ${firebase.errorCode}: ${firebase.message}"
        } else {
            "[debug] ${error.javaClass.simpleName}: ${error.message}"
        }
    }
}
