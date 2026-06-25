package com.moneyplann.app.data.api

import android.util.Log
import com.moneyplann.app.BuildConfig

object ApiDebug {
    private const val TAG = "MoneyPlanApi"

    fun logSuccess(path: String, method: String) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "$method $path OK")
    }

    fun logFailure(path: String, error: Throwable) {
        if (!BuildConfig.DEBUG) return
        Log.e(TAG, "Request failed for $path: ${error.message}", error)
    }

    fun logHttpFailure(path: String, status: Int, message: String) {
        if (!BuildConfig.DEBUG) return
        Log.e(TAG, "HTTP $status for $path: $message")
    }

    fun logDecodeFailure(path: String, bodyPreview: String, error: Throwable) {
        if (!BuildConfig.DEBUG) return
        val preview = bodyPreview.take(240)
        Log.e(TAG, "Decode failed for $path: ${error.message}; body=$preview", error)
    }
}
