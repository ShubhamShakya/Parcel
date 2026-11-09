package com.shubhamshakya.parcel.presentation
import kotlin.test.*
class InterstitialGateTest {
    @Test fun launchRepeatedTapsCooldownAndClockRollbackCannotShowAds() {
        val gate = InterstitialGate(1000)
        gate.action(); gate.action()
        assertFalse(gate.eligible(60_999))
        assertTrue(gate.eligible(61_000))
        gate.shown(61_000)
        gate.action(); gate.action()
        assertFalse(gate.eligible(180_999))
        assertFalse(gate.eligible(0))
        assertTrue(gate.eligible(181_000))
        gate.shown(181_000)
        gate.action()
        assertFalse(gate.eligible(400_000))
    }
}
