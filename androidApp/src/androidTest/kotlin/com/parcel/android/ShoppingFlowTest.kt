package com.parcel.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parcel.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class ShoppingFlowTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun onboardingSearchDetailsWishlistAndRecreation() {
        // Isolate this app's test data before opening its Activity.
        runBlocking {
            signInTestShopper()
            val dao = GlobalContext.get().get<ParcelDatabase>().shoppingDao()
            dao.setPreference(PreferenceEntity("disclosure_v1", false))
            dao.observeSavedIds().first().forEach { dao.deleteSaved(it) }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("accept_disclosure").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("A shopping simulator").assertExists()
            compose.onNodeWithTag("accept_disclosure").performScrollTo().performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("search_field").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("search_field").performTextInput("headphones")
            compose.onNodeWithTag("product_sound-one").assertExists()
            compose.onNodeWithTag("product_carry-all").assertDoesNotExist()
            compose.onNodeWithTag("product_sound-one").performScrollTo().performClick()
            compose.onNodeWithTag("product_details").assertExists()
            compose.onNodeWithTag("product_details").performScrollToIndex(3)
            compose.onNodeWithText("Sand").performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText("Colour · Sand").assertExists()
            compose.onNodeWithTag("product_details").performScrollToNode(hasTestTag("details_save"))
            compose.onNodeWithTag("details_save").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Remove from wishlist").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("product_details").performScrollToIndex(0)
            compose.onNodeWithTag("details_back").performClick()
            compose.onNodeWithTag("tab_wishlist").performClick()
            compose.onNodeWithTag("product_sound-one").assertExists()
            scenario.recreate()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("tab_wishlist").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("tab_wishlist").performClick()
            compose.onNodeWithTag("product_sound-one").assertExists()
            compose.onNodeWithTag("save_sound-one").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Make room for a favourite").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("tab_home").performClick()
            compose.onNodeWithTag("search_field").performTextReplacement("no-such-item")
            compose.onNodeWithText("No finds just yet").assertExists()
            compose.onNodeWithText("Reset filters").performClick()
            compose.onNodeWithTag("category_Home").performClick()
            compose.onNodeWithTag("product_slow-morning").assertExists()
            compose.onNodeWithTag("product_sound-one").assertDoesNotExist()
        }
    }
}
