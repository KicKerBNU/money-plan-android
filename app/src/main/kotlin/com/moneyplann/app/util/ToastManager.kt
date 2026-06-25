package com.moneyplann.app.util

import android.util.Log
import com.moneyplann.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ToastType { SUCCESS, ERROR }

data class ToastMessage(val message: String, val type: ToastType = ToastType.SUCCESS)

object ToastManager {
    private const val TAG = "MoneyPlanToast"

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    fun success(message: String) = show(message, ToastType.SUCCESS)
    fun error(message: String) = show(message, ToastType.ERROR)
    fun clear() {
        _toast.value = null
    }

    private fun show(message: String, type: ToastType) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "show type=$type message=$message")
        }
        _toast.value = ToastMessage(message, type)
    }
}
