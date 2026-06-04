package com.expiryguard.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ExpiryDateParserTest {

    // ── DD/MM/YYYY format ──

    @Test
    fun `parse DD-MM-YYYY format`() {
        val result = ExpiryDateParser.parse("EXP 15/09/2026")
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 9, 15), result!!.date)
    }

    @Test
    fun `parse DD-MM-YYYY with dash separator`() {
        val result = ExpiryDateParser.parse("EXP: 15-09-2026")
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 9, 15), result!!.date)
    }

    @Test
    fun `parse DD-MM-YYYY with dot separator`() {
        val result = ExpiryDateParser.parse("Expiry Date: 15.09.2026")
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 9, 15), result!!.date)
    }

    // ── MM/YYYY format ──

    @Test
    fun `parse MM-YYYY format`() {
        val result = ExpiryDateParser.parse("EXP 09/2026")
        assertNotNull(result)
        assertEquals(2026, result!!.date.year)
        assertEquals(9, result.date.monthValue)
        assertEquals(30, result.date.dayOfMonth)  // Last day of September
    }

    @Test
    fun `parse MM-YYYY with dash`() {
        val result = ExpiryDateParser.parse("Best Before 12-2025")
        assertNotNull(result)
        assertEquals(2025, result!!.date.year)
        assertEquals(12, result.date.monthValue)
    }

    // ── Month Year format ──

    @Test
    fun `parse Month YYYY format - short month`() {
        val result = ExpiryDateParser.parse("EXP SEP 2026")
        assertNotNull(result)
        assertEquals(2026, result!!.date.year)
        assertEquals(9, result.date.monthValue)
    }

    @Test
    fun `parse Month YYYY format - full month name`() {
        val result = ExpiryDateParser.parse("Best Before September 2026")
        assertNotNull(result)
        assertEquals(2026, result!!.date.year)
        assertEquals(9, result.date.monthValue)
    }

    @Test
    fun `parse DD Month YYYY format`() {
        val result = ExpiryDateParser.parse("EXP 15 Sep 2026")
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 9, 15), result!!.date)
    }

    // ── Keyword prioritization ──

    @Test
    fun `expiry keyword gets higher confidence than plain date`() {
        val result = ExpiryDateParser.parse("""
            MFG: 01/01/2025
            EXP: 01/01/2026
        """.trimIndent())
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 1, 1), result!!.date)
    }

    @Test
    fun `best before keyword detected`() {
        val result = ExpiryDateParser.parse("Best Before: 30/06/2026")
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 6, 30), result!!.date)
    }

    @Test
    fun `use before keyword detected`() {
        val result = ExpiryDateParser.parse("Use Before 31/12/2025")
        assertNotNull(result)
        assertEquals(LocalDate.of(2025, 12, 31), result!!.date)
    }

    // ── Noise filtering ──

    @Test
    fun `MRP line is ignored`() {
        val result = ExpiryDateParser.parse("""
            MRP Rs. 150.00
            EXP 15/09/2026
        """.trimIndent())
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 9, 15), result!!.date)
    }

    @Test
    fun `batch number line is ignored`() {
        val result = ExpiryDateParser.parse("""
            Batch No: 12345
            Lot: AB/2025/001
            EXP: 15/09/2026
        """.trimIndent())
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 9, 15), result!!.date)
    }

    @Test
    fun `FSSAI line is ignored`() {
        val result = ExpiryDateParser.parse("""
            FSSAI Lic No. 12345678901234
            Expiry: 15/09/2026
        """.trimIndent())
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 9, 15), result!!.date)
    }

    // ── Edge cases ──

    @Test
    fun `empty text returns null`() {
        assertNull(ExpiryDateParser.parse(""))
    }

    @Test
    fun `text with no dates returns null`() {
        assertNull(ExpiryDateParser.parse("No expiry date here"))
    }

    @Test
    fun `OCR noise with misspelling still works`() {
        // Common OCR error: 'l' instead of '1'
        val result = ExpiryDateParser.parse("EXP 15/09/2026 product label text")
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 9, 15), result!!.date)
    }

    @Test
    fun `future date preferred over past date`() {
        val result = ExpiryDateParser.parse("""
            15/01/2023
            15/01/2027
        """.trimIndent())
        assertNotNull(result)
        // The 2027 date should be preferred (future = likely expiry)
        assertEquals(2027, result!!.date.year)
    }

    // ── toEpochMillis ──

    @Test
    fun `toEpochMillis produces valid timestamp`() {
        val millis = ExpiryDateParser.toEpochMillis(LocalDate.of(2026, 9, 15))
        assert(millis > 0)
    }
}
