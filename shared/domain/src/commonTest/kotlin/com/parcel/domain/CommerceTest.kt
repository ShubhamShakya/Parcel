package com.parcel.domain

import kotlin.random.Random
import kotlin.test.*

class CommerceTest {
    private fun line(price: Long, quantity: Int = 1) = OrderLine("one", "Sample", "Natural", quantity, Money(price))
    @Test fun galleryHasFiveToSevenDistinctAssetsAndOffersHaveThreeToFiveCards() {
        val gallerySizes = mutableSetOf<Int>(); val deckSizes = mutableSetOf<Int>()
        repeat(100) { seed ->
            val gallery = randomGallery("mug", Random(seed)); gallerySizes += gallery.size
            assertTrue(gallery.size in 5..7); assertEquals(gallery.size, gallery.distinct().size)
            assertTrue(gallery.all { it in (0..6).map { index -> "mug_$index" } })
            val deck = randomOfferDeck(Random(seed)); deckSizes += deck.size
            assertTrue(deck.size in 3..5); assertEquals(deck.size, deck.map { it.code }.distinct().size)
        }
        assertEquals(setOf(5, 6, 7), gallerySizes); assertEquals(setOf(3, 4, 5), deckSizes)
    }
    @Test fun percentageOfferTaxAndFeeAreRoundedInMinorUnits() {
        val totals = calculateTotals(listOf(line(129900)), "EVERYDAY10")
        assertEquals(12990, totals.discount.minorUnits)
        assertEquals(21044, totals.tax.minorUnits)
        assertEquals(2900, totals.convenienceFee.minorUnits)
        assertEquals(140854, totals.total.minorUnits)
    }
    @Test fun offersRespectMinimumCapsAndDoNotStack() {
        val ineligible = calculateTotals(listOf(line(10000)), "HELLO200")
        assertFalse(ineligible.offerEligible); assertEquals(0, ineligible.discount.minorUnits)
        val capped = calculateTotals(listOf(line(1000000)), "EVERYDAY10")
        assertEquals(50000, capped.discount.minorUnits)
        val fixed = calculateTotals(listOf(line(100000)), "SMALLJOY100")
        assertEquals(10000, fixed.discount.minorUnits); assertEquals(109100, fixed.total.minorUnits)
    }
    @Test fun feeWaiverAndEmptyCartAreHandled() {
        val waived = calculateTotals(listOf(line(50000, 2)), "FEEFREE")
        assertEquals(0, waived.convenienceFee.minorUnits); assertEquals(118000, waived.total.minorUnits)
        assertEquals(0, calculateTotals(emptyList(), "FEEFREE").total.minorUnits)
    }
    @Test fun demoAddressValidationRejectsInvalidOrIncompleteDetails() {
        assertTrue(demoAddresses.all { it.validationError() == null })
        assertNotNull(demoAddresses.first().copy(postalCode = "400001").validationError())
        assertNotNull(demoAddresses.first().copy(line = "").validationError())
    }
}
