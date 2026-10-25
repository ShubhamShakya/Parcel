package com.parcel.domain

import kotlin.test.*

class AuthTest {
    @Test fun phoneBoundariesAndCountryIdentityAreExplicit() {
        assertNotNull(phoneError("1".repeat(8))); assertNotNull(phoneError("1".repeat(17)))
        assertNull(phoneError("1".repeat(9))); assertNull(phoneError("1".repeat(16)))
        assertNotNull(phoneError("12345 7890")); assertNotNull(phoneError("12345678x"))
        assertEquals("+91900000000", canonicalPhone("IN", "900000000"))
        assertNotEquals(canonicalPhone("IN", "900000000"), canonicalPhone("GB", "900000000"))
        assertEquals(canonicalPhone("US", "900000000"), canonicalPhone("CA", "900000000"))
    }
    @Test fun anyFourNonSpaceCharactersVerifyButOtherLengthsDoNot() {
        listOf("1234", "0000", "aB9!", "9876").forEach { assertNull(otpError(it)) }
        listOf("", "123", "12345", "12 4", " 123").forEach { assertNotNull(otpError(it)) }
    }
    @Test fun namesAndResendBoundaryAreValidated() {
        assertNull(nameError("  Mira ", "  Shah  "))
        assertNotNull(nameError("  ", "Shah")); assertNotNull(nameError("Mira", ""))
        assertNotNull(nameError("a".repeat(51), "Shah"))
        val challenge = OtpChallenge("+919000000000", "IN", "9000000000", 31_000)
        assertEquals(30, challenge.remainingSeconds(1000))
        assertEquals(1, challenge.remainingSeconds(30_999))
        assertEquals(0, challenge.remainingSeconds(31_000))
        assertEquals(0, challenge.remainingSeconds(100_000))
    }
}
