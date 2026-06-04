package com.expiryguard.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {

    enum class ExpiryStatus {
        EXPIRED,    // Already past expiry date
        CRITICAL,   // Within 7 days of expiry
        WARNING,    // Within 30 days of expiry
        SAFE        // More than 30 days until expiry
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    /**
     * Returns the start-of-day epoch millis for today in the device timezone.
     * Use this for ALL expiry date comparisons to avoid timezone/time-of-day bugs.
     */
    fun todayStartMillis(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Returns the number of days until the expiry date.
     * Negative values indicate the product has already expired.
     *
     * Uses date-only comparison (ignores time of day) to avoid
     * timezone bugs with DatePicker which stores dates as midnight UTC.
     */
    fun daysUntilExpiry(expiryDateMillis: Long): Long {
        val today = LocalDate.now()
        val expiryDate = Instant.ofEpochMilli(expiryDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return ChronoUnit.DAYS.between(today, expiryDate)
    }

    /**
     * Determines the expiry status based on days remaining.
     */
    fun getExpiryStatus(expiryDateMillis: Long): ExpiryStatus {
        val days = daysUntilExpiry(expiryDateMillis)
        return when {
            days < 0 -> ExpiryStatus.EXPIRED
            days <= 7 -> ExpiryStatus.CRITICAL
            days <= 30 -> ExpiryStatus.WARNING
            else -> ExpiryStatus.SAFE
        }
    }

    /**
     * Formats epoch milliseconds as "dd MMM yyyy" (e.g., "15 Jan 2025").
     */
    fun formatDate(millis: Long): String {
        val date = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return date.format(dateFormatter)
    }

    /**
     * Returns a human-readable string describing how many days remain until expiry.
     */
    fun formatDaysRemaining(expiryDateMillis: Long): String {
        val days = daysUntilExpiry(expiryDateMillis)
        return when {
            days < 0 -> "Expired ${-days} days ago"
            days == 0L -> "Expires today"
            days == 1L -> "Expires tomorrow"
            else -> "Expires in $days days"
        }
    }
}
