package com.expiryguard.app

import android.app.Application
import com.expiryguard.app.di.AppContainer
import com.expiryguard.app.util.NotificationHelper
import com.expiryguard.app.worker.ExpiryNotificationWorker

class ExpiryGuardApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createNotificationChannel(this)
        ExpiryNotificationWorker.schedule(this)
    }
}
