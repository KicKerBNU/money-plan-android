package com.moneyplann.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.moneyplann.app.core.AnalyticsHelper

class MoneyPlanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        AnalyticsHelper.init(this)
        AppContainer.init(this)
    }
}
