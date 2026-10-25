package com.parcel.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.parcel.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class CommercePersistenceTest {
    @Test fun sandboxServerCatalogMatchesTheOfflineCatalog() {
        val raw = java.io.File("../../tools/payu_catalog.json").readText()
        val fixture = kotlinx.serialization.json.Json.parseToJsonElement(raw) as kotlinx.serialization.json.JsonObject
        assertEquals(MockCatalogRepository.products.map { it.id }.toSet(), fixture.keys)
        MockCatalogRepository.products.forEach { product ->
            val item = fixture.getValue(product.id) as kotlinx.serialization.json.JsonObject
            assertEquals(product.title, (item.getValue("title") as kotlinx.serialization.json.JsonPrimitive).content)
            assertEquals(product.price.minorUnits.toString(), (item.getValue("price") as kotlinx.serialization.json.JsonPrimitive).content)
            assertEquals(product.variants, (item.getValue("variants") as kotlinx.serialization.json.JsonArray).map { (it as kotlinx.serialization.json.JsonPrimitive).content })
        }
    }

    @Test fun checkoutIsAtomicIdempotentAndSurvivesReopening(): Unit = runBlocking {
        val folder = createTempDirectory("parcel-checkout-test").toFile()
        val path = folder.resolve("shop.db").absolutePath
        fun open() = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
        var db = open()
        try {
            var repo = RoomCommerceRepository(db)
            repo.initialize(); repo.addToCart("sound-one", "Sand"); repo.addToCart("sound-one", "Sand")
            repo.selectOffer("EVERYDAY10")
            val pending = repo.prepareCheckout("checkout-one", "demo", 1000)
            assertEquals(2, pending.lines.single().quantity)
            assertEquals("checkout-one", repo.prepareCheckout("checkout-two", "demo", 2000).id)
            assertFailsWith<IllegalStateException> { repo.addToCart("carry-all", "Natural") }
            assertFailsWith<IllegalArgumentException> { repo.finalizeCheckout(pending.id, 1, "Local demo payment") }
            db.close(); db = open(); repo = RoomCommerceRepository(db)
            val restored = repo.observe().first { it.pending != null }
            assertEquals(pending, restored.pending)
            val receipt = repo.finalizeCheckout(pending.id, pending.totals.total.minorUnits, "Local demo payment")
            assertEquals(receipt, repo.finalizeCheckout(pending.id, pending.totals.total.minorUnits, "Local demo payment"))
            val completed = repo.observe().first { it.receipts.size == 1 && it.pending == null && it.cart.isEmpty() }
            assertEquals("Sand", completed.receipts.single().checkout.lines.single().variant)
            db.close(); db = open(); repo = RoomCommerceRepository(db)
            assertEquals(receipt, repo.observe().first { it.receipts.isNotEmpty() }.receipts.single())
        } finally { db.close(); folder.deleteRecursively() }
    }
    @Test fun addressCrudSelectionAndDeletionDoNotReseedDeletedAddresses(): Unit = runBlocking {
        val db = buildDatabase(Room.inMemoryDatabaseBuilder<ParcelDatabase>())
        try {
            val repo = RoomCommerceRepository(db); repo.initialize()
            assertEquals(3, repo.observe().first { it.addresses.size == 3 }.addresses.size)
            val added = demoAddresses.first().copy(id = "custom", label = "Demo office")
            repo.saveAddress(added); repo.selectAddress(added.id)
            repo.saveAddress(added.copy(line = "Demo Suite 99, Inner Circle"))
            val edited = repo.observe().first { it.selectedAddressId == "custom" && it.addresses.any { a -> a.line.startsWith("Demo Suite 99") } }
            assertEquals(4, edited.addresses.size)
            repo.deleteAddress("custom")
            assertNotEquals("custom", repo.observe().first { it.addresses.size == 3 }.selectedAddressId)
            demoAddresses.forEach { repo.deleteAddress(it.id) }; repo.initialize()
            val empty = repo.observe().first { it.addresses.isEmpty() }
            assertNull(empty.selectedAddressId)
            repo.addToCart("slow-morning", "Cream")
            assertFailsWith<IllegalStateException> { repo.prepareCheckout("no-address", "demo", 0) }
        } finally { db.close() }
    }
    @Test fun versionOneMigratesWithoutLosingWishlistOrDisclosure(): Unit = runBlocking {
        val folder = createTempDirectory("parcel-migration-test").toFile()
        val path = folder.resolve("v1.db").absolutePath
        val connection = BundledSQLiteDriver().open(path)
        listOf(
            "CREATE TABLE saved_items (productId TEXT NOT NULL, PRIMARY KEY(productId))",
            "CREATE TABLE preferences (`key` TEXT NOT NULL, value INTEGER NOT NULL, PRIMARY KEY(`key`))",
            "CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT INTO room_master_table VALUES(42, '07643502a9fb353be0be5ed7f1b7597c')",
            "INSERT INTO saved_items VALUES('sound-one')",
            "INSERT INTO preferences VALUES('disclosure_v1', 1)",
            "PRAGMA user_version = 1"
        ).forEach { sql -> connection.prepare(sql).use { it.step() } }
        connection.close()
        val db = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
        try {
            val original = RoomShoppingRepository(db.shoppingDao())
            assertEquals(setOf("sound-one"), original.observeSavedIds().first())
            assertTrue(original.observeDisclosureAccepted().first())
            val commerce = RoomCommerceRepository(db); commerce.initialize()
            assertEquals(3, commerce.observe().first { it.addresses.size == 3 }.addresses.size)
        } finally { db.close(); folder.deleteRecursively() }
    }
}
