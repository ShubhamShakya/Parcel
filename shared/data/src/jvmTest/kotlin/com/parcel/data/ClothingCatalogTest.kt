package com.parcel.data

import androidx.room.Room
import com.parcel.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class ClothingCatalogTest {
    @Test fun everySupportedDepartmentAndTypeHasACompleteProduct() {
        val products = MockCatalogRepository.products
        assertEquals(products.size, products.map { it.id }.distinct().size)
        clothingTypes.forEach { type -> type.departments.forEach { department ->
            assertTrue(products.any { it.clothing?.let { c -> c.typeId == type.id && c.department == department } == true }, "Missing $department ${type.label}")
        } }
        products.filter { it.clothing != null }.forEach { product ->
            val details = product.clothing!!
            assertEquals(details.colors.flatMap { color -> details.sizes.map { details.variant(color, it) } }.toSet(), product.variants.toSet())
            val assets = File("../presentation/src/commonMain/composeResources/files/products")
            assertTrue(File(assets, "${product.imageKey}.png").isFile, product.id)
            (0..6).forEach { assertTrue(File(assets, "${product.imageKey}_$it.png").isFile, "Missing ${product.id} image $it") }
        }
    }
    @Test fun clothingSizeAndColourSurviveCheckout(): Unit = runBlocking {
        val db = buildDatabase(Room.inMemoryDatabaseBuilder<ParcelDatabase>())
        try {
            val repository = RoomCommerceRepository(db); repository.initialize()
            val hoodie = clothingProducts.first { it.id == "men-hoodies" }
            val details = hoodie.clothing!!
            val variant = details.variant(details.colors.first(), "M")
            repository.addToCart(hoodie.id, variant)
            val draft = repository.prepareCheckout("clothing-order", "demo", 1)
            assertEquals(variant, draft.lines.single().variant)
            val receipt = repository.finalizeCheckout(draft.id, draft.totals.total.minorUnits, "Local demo payment")
            assertEquals(variant, receipt.checkout.lines.single().variant)
            assertTrue(repository.observe().first { it.receipts.isNotEmpty() }.cart.isEmpty())
        } finally { db.close() }
    }
}
