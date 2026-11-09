package com.shubhamshakya.parcel.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.shubhamshakya.parcel.data.*
import com.shubhamshakya.parcel.domain.ParcelFeatures
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

/** Online smoke test; uses Google's demo unit only, never clicks an ad.
 *  With ads switched off it asserts no slot renders and navigation still works. */
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
            if (ParcelFeatures.GOOGLE_ADS_ENABLED) {
                waitFor("native_ad_Home", 90_000)
                compose.onNodeWithTag("home_grid").performScrollToNode(hasTestTag("native_ad_Home"))
                compose.onNodeWithTag("native_ad_Home").assertIsDisplayed()
            } else compose.onNodeWithTag("native_ad_Home").assertDoesNotExist()
            compose.onNodeWithTag("home_grid").performScrollToIndex(0)
            compose.onNodeWithTag("category_Clothing").performClick()
            compose.onNodeWithTag("home_grid").performScrollToIndex(9)
            if (ParcelFeatures.GOOGLE_ADS_ENABLED) {
                waitFor("native_ad_Category", 90_000)
                compose.onNodeWithTag("native_ad_Category").assertIsDisplayed()
            } else compose.onNodeWithTag("native_ad_Category").assertDoesNotExist()
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
