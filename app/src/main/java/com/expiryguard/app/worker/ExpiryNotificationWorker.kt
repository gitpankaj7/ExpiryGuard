package com.expiryguard.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expiryguard.app.data.local.ProductEntity
import com.expiryguard.app.util.DateUtils
import com.expiryguard.app.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class ExpiryNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val firestore = FirebaseFirestore.getInstance()
        
        val now = DateUtils.todayStartMillis()
        val in30Days = now + TimeUnit.DAYS.toMillis(30)

        try {
            val snapshot = firestore.collection("users").document(uid).collection("products")
                .whereGreaterThanOrEqualTo("expiryDate", now)
                .whereLessThanOrEqualTo("expiryDate", in30Days)
                .get()
                .await()

            val products = snapshot.toObjects(ProductEntity::class.java)
            
            var notificationCount = 0
            for (product in products) {
                if (notificationCount >= 5) break // Max 5 notifications per run

                val daysRemaining = DateUtils.daysUntilExpiry(product.expiryDate)
                val riskAction = com.expiryguard.app.util.LossRecoveryEngine.analyze(listOf(product)).allActions.firstOrNull()
                
                if (daysRemaining == 30L || daysRemaining == 15L || daysRemaining == 7L || daysRemaining == 3L || daysRemaining <= 1L) {
                    val actionText = riskAction?.bestAction ?: "Purana stock sabse pehle nikalo."
                    
                    NotificationHelper.showExpiryNotification(
                        context = applicationContext,
                        productName = product.name,
                        daysRemaining = daysRemaining,
                        notificationId = product.hashCode(),
                        actionText = actionText
                    )
                    notificationCount++
                }
            }
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExpiryNotificationWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "expiry_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
