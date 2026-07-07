package com.moneyplann.app.core

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth

/** Product usage analytics via Firebase Analytics (GA4). No ads, no advertising ID. */
object AnalyticsHelper {
    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context).apply {
            setAnalyticsCollectionEnabled(true)
        }
        syncUserId()
    }

    fun syncUserId() {
        analytics?.setUserId(FirebaseAuth.getInstance().currentUser?.uid)
    }

    fun logScreen(screenName: String) {
        analytics?.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
            },
        )
    }
}
