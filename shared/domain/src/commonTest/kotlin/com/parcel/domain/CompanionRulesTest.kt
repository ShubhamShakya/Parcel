package com.parcel.domain

import kotlin.test.*

class CompanionRulesTest {
    @Test fun birthdayChecksLeapDaysAndFutureDates() {
        assertNull(profileDetailsError("Mira", "Shah", "", "2000-02-29"))
        assertNotNull(profileDetailsError("Mira", "Shah", "", "1900-02-29"))
        assertNotNull(profileDetailsError("Mira", "Shah", "", "2099-01-01"))
        assertNotNull(profileDetailsError("Mira", "Shah", "", "2000-04-31"))
        assertNotNull(profileDetailsError("Mira", "Shah", "oops", ""))
    }
    @Test fun returnDeadlineUsesActualInstantDeliveryTime() {
        val progress = DeliveryProgress("order", 1_000, "123456").deliverNow(2_000)
        assertNull(returnEligibility(progress, 2_000 + 10 * DELIVERY_DAY_MS))
        assertNotNull(returnEligibility(progress, 2_001 + 10 * DELIVERY_DAY_MS))
        assertNotNull(returnEligibility(DeliveryProgress("order", 1_000, "123456"), 2_000))
    }
    @Test fun suggestionHandlesExactSizeAndUnavailableMeasurements() {
        val measurements = listOf(DemoSizeMeasurement("S", 86.0, 70.0), DemoSizeMeasurement("M", 92.0, 76.0))
        assertEquals("S", suggestedDemoSize(measurements, 70.0))
        assertEquals("M", suggestedDemoSize(measurements, 71.0))
        assertNull(suggestedDemoSize(measurements, 77.0))
        assertNull(suggestedDemoSize(measurements, -1.0))
    }
}
