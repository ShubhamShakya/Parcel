package com.parcel.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parcel.data.*
import com.parcel.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class CompanionFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun inboxProfileAndSupportPersistAcrossRecreation() {
        val db = GlobalContext.get().get<ParcelDatabase>()
        runBlocking {
            signInTestShopper()
            db.companionDao().put(CompanionEntity("+919000000000", "{}"))
            RoomCompanionRepository(db).saveProfile("Test", "Shopper", "", "")
            db.shoppingDao().setPreference(PreferenceEntity("disclosure_v1", true))
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("home_notifications").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("home_notifications").performClick()
            compose.onNodeWithText("Mark all read").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("You’re all caught up").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithTag("tab_about").performClick()
            compose.onNodeWithText("Edit profile").performScrollTo().performClick()
            compose.onNodeWithTag("edit_first_name").performTextReplacement("Mira")
            compose.onNodeWithTag("save_profile").performScrollTo().performClick()
            compose.waitUntil(5_000) { runBlocking { RoomAuthRepository(db).observe().first().profile?.firstName == "Mira" } }
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("companion_notice_ok").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("companion_notice_ok").performClick()
            scenario.recreate()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("edit_first_name").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("edit_first_name").assertTextContains("Mira")
            compose.onNodeWithContentDescription("Back").performScrollTo().performClick()
            compose.onNodeWithText("Help centre").performScrollTo().performClick()
            compose.onNodeWithText("Chat with offline support").performScrollTo().performClick()
            compose.onNodeWithTag("chat_message").performScrollTo().performTextInput("How do returns work?")
            compose.onNodeWithTag("send_chat").performScrollTo().performClick()
            compose.waitUntil(5_000) { runBlocking { RoomCompanionRepository(db).observe().first().conversations["support"].orEmpty().any { it.text == "How do returns work?" } } }
            scenario.recreate()
            compose.waitUntil(15_000) { compose.onAllNodesWithTag("chat_message").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("How do returns work?").performScrollTo().assertIsDisplayed()
        }
    }
}
