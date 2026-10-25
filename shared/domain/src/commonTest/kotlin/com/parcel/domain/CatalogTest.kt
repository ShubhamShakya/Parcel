package com.parcel.domain

import kotlin.test.*

class CatalogTest {
    private fun product(id: String, title: String, category: Category) = Product(
        id, title, "Everyday essentials", category, Money(129900), Money(189900), "sample",
        "Sample", listOf("Natural"), listOf(Review("Sample", 5, "Example")),
    )
    private val headphones = product("audio", "Everyday Headphones", Category.Tech)
    private val tote = product("bag", "Canvas Tote", Category.Style)
    private val catalog = listOf(headphones, tote)

    @Test fun searchCombinesWordsIgnoringCaseAndWhitespace() {
        assertEquals(listOf(headphones), filterProducts(catalog, "  HEADPHONES   everyday  ", null))
    }
    @Test fun categoryAndSearchBothApply() {
        assertEquals(emptyList(), filterProducts(catalog, "headphones", Category.Style))
        assertEquals(listOf(tote), filterProducts(catalog, "", Category.Style))
    }
    @Test fun emptySearchPreservesCatalogOrder() {
        assertEquals(catalog, filterProducts(catalog, "   ", null))
    }
    @Test fun moneyUsesMinorUnitsWithoutRounding() {
        assertEquals("₹1,299", Money(129900).display())
        assertEquals("₹0.01", Money(1).display())
        assertEquals("₹12.05", Money(1205).display())
        assertFailsWith<IllegalArgumentException> { Money(-1) }
    }
    @Test fun invalidCatalogDataIsRejected() {
        assertFailsWith<IllegalArgumentException> { Review("sample", 6, "Invalid") }
        assertFailsWith<IllegalArgumentException> { headphones.copy(variants = emptyList()) }
    }
}
