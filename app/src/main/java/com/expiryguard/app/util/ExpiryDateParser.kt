package com.expiryguard.app.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Parses OCR text to extract expiry dates from product labels.
 * Optimized for Indian retail products where expiry dates are printed
 * in small fonts and inconsistent formats.
 *
 * Prioritizes:
 * - Text near keywords like EXP, EXPIRY, BEST BEFORE, USE BEFORE
 * - Dates that are in the future (likely expiry, not manufacturing)
 * - Common Indian date formats (DD/MM/YYYY, MM/YYYY, etc.)
 *
 * Ignores:
 * - MRP values (₹, Rs, MRP)
 * - Batch/Lot numbers
 * - Manufacturing dates (when expiry is available)
 */
object ExpiryDateParser {

    private const val TAG = "ExpiryDateParser"
    private const val DEBUG = false

    private fun log(message: String) {
        if (DEBUG) println("$TAG: $message")
    }

    data class ParseResult(
        val date: LocalDate,
        val confidence: Float,  // 0.0 to 1.0
        val rawMatch: String    // The original text that was matched
    )

    // Keywords that indicate expiry date (case insensitive)
    private val expiryKeywords = listOf(
        "exp", "expiry", "expiration", "expires",
        "best before", "best by", "bb",
        "use before", "use by", "ub",
        "valid till", "valid until", "valid upto",
        "consume before", "consume by",
        "shelf life", "date of expiry"
    )

    // Keywords that indicate manufacturing date (lower priority)
    private val mfgKeywords = listOf(
        "mfg", "mfd", "manufacturing", "manufactured",
        "date of manufacture", "dom", "packed on", "pkg"
    )

    // Keywords to IGNORE (these lines contain prices/batch, not dates)
    private val ignoreKeywords = listOf(
        "mrp", "₹", "rs.", "rs ", "inr", "price",
        "batch", "lot", "b.no", "l.no",
        "fssai", "lic", "licence", "license",
        "net wt", "net weight", "gst"
    )

    // Month name mappings
    private val monthNames = mapOf(
        "jan" to 1, "january" to 1,
        "feb" to 2, "february" to 2,
        "mar" to 3, "march" to 3,
        "apr" to 4, "april" to 4,
        "may" to 5,
        "jun" to 6, "june" to 6,
        "jul" to 7, "july" to 7,
        "aug" to 8, "august" to 8,
        "sep" to 9, "sept" to 9, "september" to 9,
        "oct" to 10, "october" to 10,
        "nov" to 11, "november" to 11,
        "dec" to 12, "december" to 12
    )

    /**
     * Main entry point: Parse OCR text and return the best expiry date found.
     */
    fun parse(ocrText: String): ParseResult? {
        if (ocrText.isBlank()) return null

        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val candidates = mutableListOf<ParseResult>()

        for (line in lines) {
            val lowerLine = line.lowercase(Locale.ROOT)

            // Skip lines with ignore keywords (MRP, batch, etc.)
            if (ignoreKeywords.any { lowerLine.contains(it) }) continue

            // Check if line has expiry keywords (high confidence boost)
            val hasExpiryKeyword = expiryKeywords.any { lowerLine.contains(it) }
            val hasMfgKeyword = mfgKeywords.any { lowerLine.contains(it) }

            // If it's a manufacturing date line and we have no expiry keyword, lower priority
            val keywordBoost = when {
                hasExpiryKeyword -> 0.3f
                hasMfgKeyword -> -0.3f  // Penalize mfg dates
                else -> 0.0f
            }

            // Try to extract dates from this line
            val datesFromLine = extractDates(line)
            for ((date, baseConfidence, rawMatch) in datesFromLine) {
                val adjustedConfidence = (baseConfidence + keywordBoost).coerceIn(0.0f, 1.0f)

                // Future dates are more likely to be expiry dates
                val futureBoost = if (date.isAfter(LocalDate.now())) 0.1f else -0.1f

                val finalConfidence = (adjustedConfidence + futureBoost).coerceIn(0.0f, 1.0f)

                candidates.add(ParseResult(date, finalConfidence, rawMatch))
            }
        }

        if (candidates.isEmpty()) {
            log("No dates found in OCR text")
            return null
        }

        // Sort by confidence (highest first), then by date (latest first for expiry)
        candidates.sortWith(compareByDescending<ParseResult> { it.confidence }
            .thenByDescending { it.date })

        log("Found ${candidates.size} candidates, best: ${candidates.first()}")
        return candidates.first()
    }

    /**
     * Extract all date patterns from a single line of text.
     */
    private fun extractDates(text: String): List<ParseResult> {
        val results = mutableListOf<ParseResult>()

        // Pattern 1: DD/MM/YYYY or DD-MM-YYYY or DD.MM.YYYY
        val fullDateRegex = Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})""")
        for (match in fullDateRegex.findAll(text)) {
            try {
                val day = match.groupValues[1].toInt()
                val month = match.groupValues[2].toInt()
                val year = match.groupValues[3].toInt()

                if (isValidDate(day, month, year)) {
                    val date = LocalDate.of(year, month, day)
                    results.add(ParseResult(date, 0.8f, match.value))
                }
                // Try MM/DD/YYYY if DD/MM didn't work (less common in India)
                else if (isValidDate(month, day, year)) {
                    val date = LocalDate.of(year, day, month)
                    results.add(ParseResult(date, 0.5f, match.value))
                }
            } catch (_: Exception) { }
        }

        // Pattern 2: YYYY/MM/DD or YYYY-MM-DD
        val isoDateRegex = Regex("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})""")
        for (match in isoDateRegex.findAll(text)) {
            // Make sure this wasn't already caught by Pattern 1
            if (results.any { it.rawMatch == match.value }) continue
            try {
                val year = match.groupValues[1].toInt()
                val month = match.groupValues[2].toInt()
                val day = match.groupValues[3].toInt()

                if (isValidDate(day, month, year)) {
                    val date = LocalDate.of(year, month, day)
                    results.add(ParseResult(date, 0.7f, match.value))
                }
            } catch (_: Exception) { }
        }

        // Pattern 3: MM/YYYY or MM-YYYY or MM.YYYY (common on Indian products)
        val monthYearRegex = Regex("""(?<!\d)(\d{1,2})[/\-.](\d{4})(?!\d)""")
        for (match in monthYearRegex.findAll(text)) {
            // Skip if this overlaps with a full date already matched
            if (results.any { it.rawMatch.contains(match.value) }) continue
            try {
                val month = match.groupValues[1].toInt()
                val year = match.groupValues[2].toInt()

                if (month in 1..12 && year in 2020..2040) {
                    // Use last day of month for MM/YYYY
                    val date = LocalDate.of(year, month, 1).withDayOfMonth(
                        LocalDate.of(year, month, 1).lengthOfMonth()
                    )
                    results.add(ParseResult(date, 0.7f, match.value))
                }
            } catch (_: Exception) { }
        }

        // Pattern 4: Month YYYY or YYYY Month (e.g., SEP 2026, September 2026)
        val cleanText = text.lowercase(Locale.ROOT)
        for ((monthName, monthNum) in monthNames) {
            // Month YYYY (but NOT if preceded by a digit — that's DD Month YYYY handled below)
            val monthYearNameRegex = Regex("""$monthName[.,]?\s*(\d{4})""", RegexOption.IGNORE_CASE)
            for (match in monthYearNameRegex.findAll(cleanText)) {
                // Skip if preceded by a digit (e.g., "15 sep 2026" → handled by DD Month YYYY)
                val startIdx = match.range.first
                if (startIdx > 0) {
                    val preceding = cleanText.substring(maxOf(0, startIdx - 3), startIdx).trimEnd()
                    if (preceding.isNotEmpty() && preceding.last().isDigit()) continue
                }
                try {
                    val year = match.groupValues[1].toInt()
                    if (year in 2020..2040) {
                        val date = LocalDate.of(year, monthNum, 1).withDayOfMonth(
                            LocalDate.of(year, monthNum, 1).lengthOfMonth()
                        )
                        results.add(ParseResult(date, 0.75f, match.value))
                    }
                } catch (_: Exception) { }
            }

            // DD Month YYYY (e.g., 15 Sep 2026)
            val dayMonthYearRegex = Regex("""(\d{1,2})\s*$monthName[.,]?\s*(\d{4})""", RegexOption.IGNORE_CASE)
            for (match in dayMonthYearRegex.findAll(cleanText)) {
                try {
                    val day = match.groupValues[1].toInt()
                    val year = match.groupValues[2].toInt()
                    if (isValidDate(day, monthNum, year)) {
                        val date = LocalDate.of(year, monthNum, day)
                        results.add(ParseResult(date, 0.85f, match.value))
                    }
                } catch (_: Exception) { }
            }

            // Month DD, YYYY (e.g., Sep 15, 2026)
            val monthDayYearRegex = Regex("""$monthName[.,]?\s*(\d{1,2})[,.]?\s*(\d{4})""", RegexOption.IGNORE_CASE)
            for (match in monthDayYearRegex.findAll(cleanText)) {
                try {
                    val day = match.groupValues[1].toInt()
                    val year = match.groupValues[2].toInt()
                    if (isValidDate(day, monthNum, year)) {
                        val date = LocalDate.of(year, monthNum, day)
                        results.add(ParseResult(date, 0.8f, match.value))
                    }
                } catch (_: Exception) { }
            }
        }

        // Pattern 5: MMYYYY without separator (e.g., 092026 for Sep 2026)
        val compactRegex = Regex("""(?<!\d)(\d{2})(\d{4})(?!\d)""")
        for (match in compactRegex.findAll(text)) {
            try {
                val month = match.groupValues[1].toInt()
                val year = match.groupValues[2].toInt()
                if (month in 1..12 && year in 2020..2040) {
                    val date = LocalDate.of(year, month, 1).withDayOfMonth(
                        LocalDate.of(year, month, 1).lengthOfMonth()
                    )
                    results.add(ParseResult(date, 0.4f, match.value))
                }
            } catch (_: Exception) { }
        }

        return results
    }

    private fun isValidDate(day: Int, month: Int, year: Int): Boolean {
        if (year !in 2020..2040) return false
        if (month !in 1..12) return false
        if (day !in 1..31) return false
        return try {
            LocalDate.of(year, month, day)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Convert a LocalDate to epoch milliseconds (for Room storage).
     */
    fun toEpochMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
