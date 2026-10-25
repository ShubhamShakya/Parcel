package com.parcel.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.parcel.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/** Online test using Google demo units; never clicks advertisements. */
class SectionBannerAdsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun waitFor(tag: String, timeout: Long = 20_000) = compose.waitUntil(timeout) {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
    @Test fun bannersLoadInRequestedTabsAndStayOutOfCheckout() {
        runBlocking {
            signInTestShopper()
            val db = GlobalContext.get().get<ParcelDatabase>()
            db.shoppingDao().setPreference(PreferenceEntity("disclosure_v1", true))
            db.commerceDao().clearPending()
            val commerce = RoomCommerceRepository(db)
            commerce.initialize()
            if (db.commerceDao().addresses().isEmpty()) commerce.saveAddress(com.parcel.domain.demoAddresses.first())
            commerce.addToCart("sound-one", "Sand")
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor("tab_wishlist")
            listOf("wishlist" to "Wishlist", "cart" to "Cart", "purchased" to "Purchased").forEach { (tab, placement) ->
                compose.onNodeWithTag("tab_$tab").performClick()
                waitFor("banner_ad_$placement", 90_000)
                compose.onNodeWithTag("banner_ad_$placement").assertIsDisplayed()
            }
            scenario.recreate()
            waitFor("banner_ad_Purchased", 90_000)
            compose.onNodeWithTag("tab_cart").performClick()
            waitFor("cart_checkout")
            compose.onNodeWithTag("cart_checkout").performClick()
            compose.onNodeWithTag("banner_ad_Cart").assertDoesNotExist()
        }
    }
}
