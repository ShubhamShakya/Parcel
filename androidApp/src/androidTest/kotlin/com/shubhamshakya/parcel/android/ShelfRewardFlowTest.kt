package com.shubhamshakya.parcel.android

import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.shubhamshakya.parcel.data.*
import com.shubhamshakya.parcel.domain.*
import com.shubhamshakya.parcel.presentation.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

class ShelfRewardFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun waitFor(tag: String) = compose.waitUntil(20_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    @Test fun optionalRewardUnlocksOneSpaceAndSurvivesRecreation() {
        val db = GlobalContext.get().get<ParcelDatabase>()
        val repo = RoomDeliveryRepository(db)
        runBlocking {
            signInTestShopper()
            db.shoppingDao().setPreference(PreferenceEntity("disclosure_v1", true))
            for (slot in 6..11) {
                repo.clearSlot(slot)
                db.shoppingDao().setPreference(PreferenceEntity("shelf_unlocked_$slot", false))
            }
            repo.refresh()
            assertEquals(6, repo.observe().first().unlockedSlots.size)
        }
        var requests = 0
        var rewardSlot = -1
        var completion: ((String?) -> Unit)? = null
        val fake = object : ParcelAds {
            @Composable override fun NativeCard(placement: AdPlacement) = Unit
            @Composable override fun PrivacyOptions() = Unit
            override fun watchShelfReward(slot: Int, finished: (String?) -> Unit): () -> Unit {
                requests++; rewardSlot = slot; completion = finished
                return { completion = null }
            }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val vm = DeliveryViewModel(repo)
            scenario.onActivity { activity -> activity.setContent {
                val state by vm.state.collectAsState()
                ParcelTheme { CompositionLocalProvider(LocalParcelAds provides fake) {
                    PurchasedScreen(emptyList(), emptyList(), state, vm, {})
                } }
            } }
            if (ParcelFeatures.GOOGLE_ADS_ENABLED) {
                waitFor("shelf_slot_6")
                compose.onNodeWithTag("purchased_list").performScrollToNode(hasTestTag("shelf_slot_6"))
                compose.onNodeWithTag("shelf_slot_6").performClick()
                compose.onNodeWithTag("cancel_shelf_ad").performClick()
                assertEquals(0, requests)
                compose.onNodeWithTag("shelf_slot_6").performClick()
                compose.onNodeWithTag("watch_shelf_ad").performClick()
                assertEquals(1, requests)
                assertEquals(6, rewardSlot)
                assertEquals(6, runBlocking { repo.observe().first().unlockedSlots.size })
                scenario.onActivity { completion?.invoke("Ad closed before earning the reward.") }
                compose.onNodeWithText("OK").performClick()
                assertEquals(6, runBlocking { repo.observe().first().unlockedSlots.size })
                compose.onNodeWithTag("shelf_slot_6").performClick()
                compose.onNodeWithTag("watch_shelf_ad").performClick()
                runBlocking { repo.unlockSlot(rewardSlot); repo.unlockSlot(rewardSlot) }
                scenario.onActivity { completion?.invoke(null) }
                compose.onNodeWithText("OK").performClick()
            } else {
                // With rewarded ads switched off a locked space can never be unlocked, so the
                // shelf shows the six free spaces only and asks for no ad.
                waitFor("shelf_slot_0")
                compose.onNodeWithTag("shelf_slot_5").assertExists()
                compose.onNodeWithTag("shelf_slot_6").assertDoesNotExist()
                compose.onNodeWithTag("shelf_slot_11").assertDoesNotExist()
                assertEquals(0, requests)
                // An entitlement earned before ads were switched off still shows.
                runBlocking { repo.unlockSlot(6) }
            }
            compose.waitUntil(10_000) { runBlocking { repo.observe().first().unlockedSlots.size == 7 } }
            scenario.recreate()
            waitFor("tab_purchased")
            compose.onNodeWithTag("tab_purchased").performClick()
            waitFor("purchased_list")
            compose.onNodeWithTag("purchased_list").performScrollToNode(hasTestTag("shelf_slot_6"))
            compose.onNodeWithTag("shelf_slot_6").assertTextContains("Space 7")
            assertEquals(7, runBlocking { repo.observe().first().unlockedSlots.size })
        }
    }
}
