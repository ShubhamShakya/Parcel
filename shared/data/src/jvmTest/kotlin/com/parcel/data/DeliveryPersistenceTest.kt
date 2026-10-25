package com.parcel.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.parcel.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class DeliveryPersistenceTest {
    @Test fun offlineProgressOtpUnboxingAndShelfSurviveReopening(): Unit = runBlocking {
        val folder = createTempDirectory("parcel-delivery").toFile()
        val path = folder.resolve("shop.db").absolutePath
        var time = 1_000L
        fun open() = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
        var db = open()
        try {
            val commerce = RoomCommerceRepository(db) { time }
            commerce.initialize(); commerce.addToCart("sound-one", "Sand"); commerce.addToCart("sound-one", "Sand")
            val pending = commerce.prepareCheckout("tracked", "demo", 0)
            val receipt = commerce.finalizeCheckout(pending.id, pending.totals.total.minorUnits, "Local demo payment")
            var repo = RoomDeliveryRepository(db) { time }
            val original = repo.observe().first().orders.single()
            val otp = original.otp
            assertTrue(otp.matches(Regex("[0-9]{6}")))
            assertEquals(time, repo.observe().first().orders.single().confirmedAt)
            assertFailsWith<IllegalStateException> { repo.unbox("tracked") }
            assertFailsWith<IllegalStateException> { repo.place(0, purchasedItems(receipt).first().id) }
            db.close(); time += DELIVERY_DAY_MS; db = open(); repo = RoomDeliveryRepository(db) { time }
            repo.refresh()
            assertEquals(DeliveryStage.Delivered, repo.observe().first().orders.single().stage)
            assertEquals(otp, repo.observe().first().orders.single().otp)
            assertEquals(original.scheduleDurationMs, repo.observe().first().orders.single().scheduleDurationMs)
            repo.unbox("tracked"); repo.unbox("tracked")
            val items = purchasedItems(receipt)
            repo.place(0, items[0].id); repo.place(1, items[1].id)
            repo.place(1, items[0].id) // Move one unit, replacing another without losing ownership.
            assertEquals(listOf(ShelfPlacement(1, items[0].id)), repo.observe().first().shelf)
            repo.place(11, items[1].id)
            assertFailsWith<IllegalArgumentException> { repo.place(12, items[0].id) }
            assertFailsWith<IllegalStateException> { repo.place(3, "not-owned") }
            db.close(); db = open(); repo = RoomDeliveryRepository(db) { 0 }
            repo.refresh()
            val restored = repo.observe().first()
            assertEquals(2, restored.shelf.size)
            assertNotNull(restored.orders.single().unboxedAt)
            assertEquals(DeliveryStage.Delivered, restored.orders.single().stage)
            repo.clearSlot(1)
            assertEquals(1, repo.observe().first().shelf.size)
        } finally { db.close(); folder.deleteRecursively() }
    }
    @Test fun instantDeliverySurvivesRefreshAndCannotDuplicatePurchasedUnits(): Unit = runBlocking {
        val db = buildDatabase(Room.inMemoryDatabaseBuilder<ParcelDatabase>())
        try {
            val commerce = RoomCommerceRepository(db) { 1000 }
            commerce.initialize(); commerce.addToCart("slow-morning", "Cream")
            val pending = commerce.prepareCheckout("instant", "demo", 0)
            commerce.finalizeCheckout(pending.id, pending.totals.total.minorUnits, "Local demo payment")
            val repo = RoomDeliveryRepository(db) { 2000 }
            repo.deliverNow("instant"); repo.deliverNow("instant"); repo.refresh(); repo.unbox("instant"); repo.unbox("instant")
            val progress = repo.observe().first().orders.single()
            assertEquals(2000, progress.instantDeliveredAt)
            assertEquals(2000, progress.unboxedAt)
            assertEquals(1, commerce.observe().first().receipts.flatMap(::purchasedItems).size)
        } finally { db.close() }
    }
    @Test fun versionTwoMigratesReceiptsAndCartWithoutLosingTheirData(): Unit = runBlocking {
        val folder = createTempDirectory("parcel-v2").toFile()
        val path = folder.resolve("shop.db").absolutePath
        val schema = Json.parseToJsonElement(java.io.File("schemas/com.parcel.data.ParcelDatabase/2.json").readText()).jsonObject.getValue("database").jsonObject
        val connection = BundledSQLiteDriver().open(path)
        fun sql(value: String) = connection.prepare(value).use { it.step() }
        schema.getValue("entities").jsonArray.forEach { entity ->
            val row = entity.jsonObject
            sql(row.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", row.getValue("tableName").jsonPrimitive.content))
        }
        val line = OrderLine("slow-morning", "Slow morning mug", "Cream", 1, Money(79900))
        val receipt = Receipt(PendingCheckout("legacy", "demo", listOf(line), demoAddresses.first(), calculateTotals(listOf(line), null), 1000), "Local demo payment")
        sql("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        sql("INSERT INTO room_master_table VALUES(42, '${schema.getValue("identityHash").jsonPrimitive.content}')")
        sql("INSERT INTO cart VALUES('sound-one', 'Sand', 2)")
        connection.prepare("INSERT INTO receipts VALUES(?, ?, ?)").use { it.bindText(1,"legacy"); it.bindLong(2,1000); it.bindText(3,Json.encodeToString(receipt)); it.step() }
        sql("PRAGMA user_version = 2"); connection.close()
        val db = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
        try {
            val commerce = RoomCommerceRepository(db)
            assertEquals(2, commerce.observe().first().cart.single().quantity)
            assertEquals(receipt, commerce.observe().first().receipts.single())
            val repo = RoomDeliveryRepository(db) { 1000 + DELIVERY_DAY_MS }
            repo.refresh()
            assertEquals(DeliveryStage.Delivered, repo.observe().first().orders.single().stage)
            assertNull(repo.observe().first().orders.single().unboxedAt)
        } finally { db.close(); folder.deleteRecursively() }
    }
}
