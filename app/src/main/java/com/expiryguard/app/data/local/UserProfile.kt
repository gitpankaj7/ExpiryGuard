package com.expiryguard.app.data.local


data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val trialStart: Long = 0L,
    val isSubscribed: Boolean = false,
    val subscriptionEnd: Long? = null
) {
    fun getTrialDaysRemaining(): Int {
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        val timePassed = now - trialStart
        if (timePassed > thirtyDaysInMillis) return 0
        return ((thirtyDaysInMillis - timePassed) / (1000 * 60 * 60 * 24)).toInt()
    }

    fun isTrialExpired(): Boolean {
        return getTrialDaysRemaining() <= 0
    }
}
