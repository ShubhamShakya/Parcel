package com.parcel.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parcel.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class CheckoutFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun waitFor(tag: String) = compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun clickIn(list: String, tag: String) {
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performClick()
    }
    @Test fun offersGalleryCartAddressesAndLocalCheckout() {
        runBlocking {
            signInTestShopper()
            val db = GlobalContext.get().get<ParcelDatabase>()
            db.shoppingDao().setPreference(PreferenceEntity("disclosure_v1", true))
            val dao = db.commerceDao()
            dao.clearPending()
            dao.cart().forEach { dao.removeCart(it.productId, it.variant) }
            dao.addresses().forEach { dao.removeAddress(it.id) }
            dao.putSettings(CheckoutSettingEntity())
            RoomCommerceRepository(db).initialize()
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor("offer_carousel")
            val pager = compose.onNodeWithTag("offer_carousel")
            fun indicator() = compose.onAllNodesWithTag("offer_dots", useUnmergedTree = true)
                .fetchSemanticsNodes().first().config[SemanticsProperties.ContentDescription].single()
            val initial = indicator()
            val cardCount = initial.substringAfter(" of ").toInt()
            assertTrue(cardCount in 3..5)
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(5_600)
            compose.mainClock.autoAdvance = true
            compose.waitUntil(5_000) { indicator() != initial }
            compose.onNodeWithText("offers · Swipe in either direction", substring = true).assertDoesNotExist()
            repeat(cardCount) { pager.performTouchInput { swipeLeft() } }
            compose.onNodeWithTag("offer_carousel").performTouchInput { swipeRight() }
            compose.onNodeWithTag("offer_carousel").performTouchInput { click(center) }
            waitFor("notice_ok"); compose.onNodeWithTag("notice_ok").performClick()
            compose.onNodeWithTag("search_field").performTextInput("headphones")
            clickIn("home_grid", "product_sound-one")
            waitFor("product_gallery")
            val text = compose.onNodeWithTag("gallery_count").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
            assertTrue(text, Regex(".*Image 1 of [567].*").matches(text))
            compose.onNodeWithTag("thumbnail_1").performClick()
            compose.onNodeWithTag("gallery_count").assertTextContains("Image 2", substring = true)
            val galleryBefore = compose.onNodeWithTag("gallery_count").fetchSemanticsNode().config[SemanticsProperties.Text].single().text
            scenario.recreate(); waitFor("product_gallery")
            compose.onNodeWithTag("gallery_count").assertTextEquals(galleryBefore)
            compose.onNodeWithText("Sand").performScrollTo().performClick()
            compose.onNodeWithTag("details_add_cart").performClick()
            waitFor("notice_ok"); compose.onNodeWithTag("notice_ok").performClick()
            compose.onNodeWithTag("product_details").performScrollToIndex(0)
            compose.onNodeWithTag("details_back").performClick()
            compose.onNodeWithTag("tab_cart").performClick()
            compose.onNodeWithTag("increase_sound-one").performClick()
            compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("quantity_sound-one") and hasText("2")).fetchSemanticsNodes().isNotEmpty() }
            scenario.recreate(); waitFor("tab_cart")
            compose.onNodeWithTag("tab_cart").performClick()
            compose.onNodeWithTag("quantity_sound-one").assertTextEquals("2")
            compose.onNodeWithTag("cart_checkout").performClick()
            clickIn("address_list", "edit_address_demo-cp")
            compose.onNodeWithTag("address_label").performTextReplacement("Demo central")
            compose.onNodeWithTag("address_line").performScrollTo().performTextReplacement("Demo Suite 99, Block A")
            compose.onNodeWithTag("save_address").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Demo central").fetchSemanticsNodes().isNotEmpty() }
            clickIn("address_list", "delete_address_demo-bengali")
            compose.onNodeWithTag("confirm_delete_address").performClick()
            compose.onNodeWithTag("address_list").performScrollToIndex(0)
            compose.onNodeWithTag("add_address").performClick()
            compose.onNodeWithTag("address_label").performTextReplacement("Demo extra")
            compose.onNodeWithTag("address_line").performScrollTo().performTextReplacement("Demo Flat 7, Inner Circle")
            compose.onNodeWithTag("save_address").performClick()
            compose.waitForIdle()
            clickIn("address_list", "address_continue")
            compose.onNodeWithTag("checkout_list").performScrollToNode(hasText("Applied automatically", substring = true))
            compose.onNodeWithText("Applied automatically", substring = true).assertExists()
            compose.onNodeWithTag("checkout_list").performScrollToNode(hasText("Tax · 18%"))
            compose.onNodeWithText("Convenience fee").assertExists()
            clickIn("checkout_list", "checkout_acknowledge")
            clickIn("checkout_list", "method_payu")
            compose.onNodeWithTag("payu_setup_status").assertExists()
            compose.onNodeWithTag("place_order").assertIsNotEnabled()
            clickIn("checkout_list", "method_demo")
            compose.onNodeWithTag("place_order").performClick()
            waitFor("receipt_list")
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Payment confirmed · no charge").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Payment confirmed · no charge").assertExists()
        }
    }
}
