package com.parcel.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.parcel.data.*
import com.parcel.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext

class DeliveryFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun waitFor(tag: String) = compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    private fun clickIn(list: String, tag: String) {
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag)); compose.onNodeWithTag(tag).performClick()
    }
    @Test fun deliverUnboxAndArrangeShelfAcrossRecreation() {
        val orderId = "delivery-ui-${System.currentTimeMillis()}"
        lateinit var receipt: Receipt
        runBlocking {
            signInTestShopper()
            val db = GlobalContext.get().get<ParcelDatabase>()
            db.shoppingDao().setPreference(PreferenceEntity("disclosure_v1", true))
            db.commerceDao().clearPending()
            db.commerceDao().cart().forEach { db.commerceDao().removeCart(it.productId, it.variant) }
            val commerce = RoomCommerceRepository(db)
            commerce.initialize()
            if (db.commerceDao().addresses().isEmpty()) commerce.saveAddress(demoAddresses.first())
            commerce.addToCart("sound-one", "Sand"); commerce.addToCart("sound-one", "Sand")
            val pending = commerce.prepareCheckout(orderId, "demo", System.currentTimeMillis())
            receipt = commerce.finalizeCheckout(orderId, pending.totals.total.minorUnits, "Local demo payment")
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor("tab_purchased"); compose.onNodeWithTag("tab_purchased").performClick()
            compose.onNodeWithTag("purchased_orders").performClick()
            compose.onNodeWithTag("order_$orderId").performClick()
            waitFor("receipt_list")
            compose.onNodeWithTag("receipt_list").performScrollToNode(hasTestTag("delivery_otp"))
            compose.onNodeWithTag("delivery_otp").assertExists()
            clickIn("receipt_list", "deliver_now")
            waitFor("open_unboxing")
            scenario.recreate(); waitFor("receipt_list")
            clickIn("receipt_list", "open_unboxing")
            waitFor("start_unboxing")
            compose.onNodeWithTag("start_unboxing").performClick()
            waitFor("unboxing_to_shelf")
            compose.onNodeWithTag("unboxing_to_shelf").performScrollTo().performClick()
            waitFor("shelf_slot_0")
            compose.onNodeWithTag("shelf_slot_0").performClick()
            val units = purchasedItems(receipt)
            compose.onNodeWithTag("pick_${units[0].id}").assertExists().performClick()
            waitFor("shelf_item_${units[0].id}")
            scenario.recreate(); waitFor("tab_purchased")
            compose.onNodeWithTag("tab_purchased").performClick()
            waitFor("shelf_item_${units[0].id}")
            compose.onNodeWithTag("shelf_slot_1").performClick()
            compose.onNodeWithTag("pick_${units[0].id}").performClick()
            compose.waitUntil(5000) { compose.onAllNodes(hasTestTag("shelf_slot_0") and hasText("Space 1")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("shelf_slot_1").performClick()
            compose.onNodeWithTag("clear_shelf_slot").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithTag("shelf_item_${units[0].id}", useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("shelf_slot_0").performClick()
            compose.onNodeWithTag("pick_${units[0].id}").assertExists()
            compose.onNodeWithTag("pick_${units[1].id}").assertExists()
        }
    }
}
