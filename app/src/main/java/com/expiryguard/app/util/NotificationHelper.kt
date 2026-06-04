package com.expiryguard.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.expiryguard.app.MainActivity
import com.expiryguard.app.R

object NotificationHelper {

    const val CHANNEL_ID = "expiry_alerts"

    /**
     * Creates the notification channel for expiry alerts.
     * Safe to call multiple times — the system ignores duplicate channel creation.
     */
    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Expiry Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for products nearing expiry or already expired"
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Shows a notification for a product nearing expiry or already expired.
     */
    fun showExpiryNotification(
        context: Context,
        productName: String,
        daysRemaining: Long,
        notificationId: Int
    ) {
        val title = when {
            daysRemaining <= 0 -> "🚨 Product Expired!"
            daysRemaining == 1L -> "⚠️ Expiring Tomorrow!"
            daysRemaining <= 7 -> "⏰ Expiring Soon"
            else -> "📦 Expiry Reminder"
        }

        val text = when {
            daysRemaining < 0 -> "$productName expired ${-daysRemaining} days ago"
            daysRemaining == 0L -> "$productName expires today"
            daysRemaining == 1L -> "$productName expires tomorrow"
            else -> "$productName expires in $daysRemaining days"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
