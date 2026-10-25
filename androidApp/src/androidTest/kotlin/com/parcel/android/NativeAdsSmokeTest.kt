package com.parcel.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.parcel.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/** Online smoke test; uses Google's demo unit only, never clicks an ad. */
class NativeAdsSmokeTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun waitFor(tag: String, timeout: Long = 20_000) = compose.waitUntil(timeout) {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
    @Test fun testNativeAdsLoadInHomeAndClothingAndNavigationRemainsUsable() {
        runBlocking {
            signInTestShopper()
            GlobalContext.get().get<ParcelDatabase>().shoppingDao().setPreference(PreferenceEntity("disclosure_v1", true))
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor("home_grid")
            waitFor("native_ad_Home", 90_000)
            compose.onNodeWithTag("home_grid").performScrollToNode(hasTestTag("native_ad_Home"))
            compose.onNodeWithTag("native_ad_Home").assertIsDisplayed()
            compose.onNodeWithTag("home_grid").performScrollToIndex(0)
            compose.onNodeWithTag("category_Clothing").performClick()
            compose.onNodeWithTag("home_grid").performScrollToIndex(9)
            waitFor("native_ad_Category", 90_000)
            compose.onNodeWithTag("native_ad_Category").assertIsDisplayed()
            compose.onNodeWithTag("tab_purchased").performClick()
            waitFor("purchased_orders")
            compose.onNodeWithTag("native_ad_Category").assertDoesNotExist()
            scenario.recreate()
            waitFor("tab_home")
            compose.onNodeWithTag("tab_home").performClick()
            waitFor("home_grid")
        }
    }
}
