package com.expiryguard.app.data.local

import java.util.Date

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val trialStart: Date? = null,
    val isSubscribed: Boolean = false,
    val subscriptionEnd: Long? = null
) {
    fun getTrialDaysRemaining(): Int {
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        val startMillis = trialStart?.time ?: now // default to now if null
        val timePassed = now - startMillis
        if (timePassed > thirtyDaysInMillis) return 0
        return ((thirtyDaysInMillis - timePassed) / (1000 * 60 * 60 * 24)).toInt()
    }

    fun isTrialExpired(): Boolean {
        return getTrialDaysRemaining() <= 0
    }
}
