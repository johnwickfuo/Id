package com.africodeai.id

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LicenseTest {

    @Test
    fun generateUsesStateAndInitials() {
        val license = generateLicense("CA", "Jane", "Doe")
        assertTrue(license.startsWith("CA-JD-"), "unexpected format: $license")
        val digits = license.substringAfterLast("-")
        assertEquals(6, digits.length)
        assertTrue(digits.all { it.isDigit() })
    }

    @Test
    fun generateFallsBackToXForEmptyNames() {
        val license = generateLicense("NY", "", "")
        assertTrue(license.startsWith("NY-XX-"), "unexpected format: $license")
    }

    @Test
    fun validateAcceptsAMatchingPattern() {
        // CA pattern: ^CA[A-Z]{5}\d{2}\d{6}$
        assertTrue(validateLicenseNumber("CA", "CAABCDE12123456"))
    }

    @Test
    fun validateRejectsWrongPattern() {
        assertFalse(validateLicenseNumber("CA", "CA-JD-123456"))
    }

    @Test
    fun validateRejectsUnknownState() {
        assertFalse(validateLicenseNumber("ZZ", "ZZABCDE12123456"))
    }
}
