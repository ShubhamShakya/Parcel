package com.parcel.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.parcel.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class ClothingFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private fun waitFor(tag: String) = compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun clickIn(list: String, tag: String) {
        compose.onNodeWithTag(list).performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().performClick()
    }
    @Test fun departmentsWinterFiltersAndSizeAwareCart() {
        runBlocking {
            signInTestShopper()
            val db = GlobalContext.get().get<ParcelDatabase>()
            db.shoppingDao().setPreference(PreferenceEntity("disclosure_v1", true))
            db.commerceDao().clearPending()
            db.commerceDao().cart().forEach { db.commerceDao().removeCart(it.productId, it.variant) }
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("category_Clothing")
            compose.onNodeWithTag("category_Clothing").performClick()
            compose.onNodeWithTag("department_Men").assertIsSelected()
            compose.onNodeWithTag("quick_type_shirts").performClick()
            compose.onNodeWithTag("product_men-shirts").assertExists()
            compose.onNodeWithTag("product_women-shirts").assertDoesNotExist()
            compose.onNodeWithTag("winterwear_filter").performClick()
            compose.onNodeWithTag("quick_type_hoodies").performClick()
            clickIn("home_grid", "product_men-hoodies")
            compose.onNodeWithTag("details_add_cart").assertIsNotEnabled()
            clickIn("product_details", "clothing_size_M")
            compose.onNodeWithTag("details_add_cart").assertIsEnabled().performClick()
            waitFor("notice_ok"); compose.onNodeWithTag("notice_ok").performClick()
            compose.onNodeWithTag("product_details").performScrollToIndex(0)
            compose.onNodeWithTag("details_back").performClick()
            compose.onNodeWithTag("tab_cart").performClick()
            val hoodie = MockCatalogRepository.products.first { p -> p.id == "men-hoodies" }.clothing!!
            compose.onNodeWithText("${hoodie.colors.first()} / M").assertExists()
            compose.onNodeWithTag("tab_home").performClick()
            compose.onNodeWithTag("home_grid").performScrollToIndex(0)
            compose.onNodeWithTag("department_Women").performClick()
            compose.onNodeWithTag("winterwear_filter").assertIsNotSelected()
            compose.onNodeWithTag("quick_type_dresses").performClick()
            compose.onNodeWithTag("product_women-dresses").assertExists()
            compose.onNodeWithTag("product_men-hoodies").assertDoesNotExist()
            compose.onNodeWithTag("clothing_filters").performClick()
            compose.onNodeWithTag("facet_size_M").performClick()
            val dress = MockCatalogRepository.products.first { p -> p.id == "women-dresses" }.clothing!!
            clickIn("clothing_filter_list", "facet_color_${dress.colors.first()}")
            clickIn("clothing_filter_list", "facet_fabric_${dress.fabric}")
            compose.onNodeWithTag("apply_clothing_filters").performClick()
            compose.onNodeWithTag("product_women-dresses").assertExists()
            compose.onNodeWithTag("clear_clothing_filters").performClick()
            compose.onNodeWithTag("more_clothing_types").performClick()
            clickIn("clothing_type_list", "type_sarees")
            clickIn("home_grid", "product_women-sarees")
            clickInDetailsWithoutClick("clothing_size_One size")
            compose.onNodeWithTag("clothing_size_One size").assertIsSelected()
            compose.onNodeWithTag("details_add_cart").assertIsEnabled().performClick()
            waitFor("notice_ok"); compose.onNodeWithTag("notice_ok").performClick()
        }
    }
    private fun clickInDetailsWithoutClick(tag: String) {
        compose.onNodeWithTag("product_details").performScrollToNode(hasTestTag(tag))
    }
}
