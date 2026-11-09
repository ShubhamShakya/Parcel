package com.shubhamshakya.parcel.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.shubhamshakya.parcel.data.*
import com.shubhamshakya.parcel.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

class AppearanceNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun waitFor(tag: String) = compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    @Test fun homeRemainsReachableAndAppearanceSurvivesRecreation() {
        val db = GlobalContext.get().get<ParcelDatabase>()
        val preferences = RoomShoppingRepository(db.shoppingDao())
        runBlocking {
            signInTestShopper()
            db.shoppingDao().setPreference(PreferenceEntity("disclosure_v1", true))
            preferences.setAppearance(AppearanceMode.Dark)
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor("tab_home")
            repeat(3) {
                compose.onNodeWithTag("tab_purchased").performClick()
                waitFor("purchased_orders")
                compose.onNodeWithTag("tab_home").performClick()
                waitFor("search_field")
                compose.onNodeWithTag("search_field").assertIsDisplayed()
                compose.onNodeWithTag("purchased_orders").assertDoesNotExist()
                compose.onNodeWithTag("tab_home").assertIsSelected()
            }
            compose.onNodeWithTag("search_field").performTextInput("headphones")
            compose.onNodeWithContentDescription("Clear search").performClick()
            compose.onNodeWithTag("clear_search").assertDoesNotExist()
            compose.onNodeWithTag("tab_about").performClick()
            compose.onNodeWithTag("appearance_dark").performScrollTo().assertIsSelected()
            compose.onNodeWithTag("appearance_light").performScrollTo().performClick()
            compose.waitUntil(5_000) { runBlocking { preferences.observeAppearance().first() == AppearanceMode.Light } }
            scenario.recreate()
            waitFor("tab_about")
            compose.onNodeWithTag("appearance_light").performScrollTo().assertIsSelected()
            compose.onNodeWithTag("tab_home").performClick()
            waitFor("search_field")
            compose.onNodeWithTag("tab_about").performClick()
            compose.onNodeWithTag("appearance_system").performScrollTo().performClick()
            compose.waitUntil(5_000) { runBlocking { preferences.observeAppearance().first() == AppearanceMode.System } }
        }
    }
}
