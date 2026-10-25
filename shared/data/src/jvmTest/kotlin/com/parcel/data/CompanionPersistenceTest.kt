package com.parcel.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.parcel.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class CompanionPersistenceTest {
    private suspend fun login(db: ParcelDatabase, number: String = "9000000000") {
        val auth = RoomAuthRepository(db)
        auth.signOut(); auth.requestOtp("IN", number); auth.verifyOtp("1234")
        if (auth.observe().first().profile == null) auth.register("Test", "Shopper")
    }
    @Test fun accountChangesSurviveReopenAndStayWithTheirProfile(): Unit = runBlocking {
        val folder = createTempDirectory("parcel-companion").toFile()
        val path = folder.resolve("shop.db").absolutePath
        var db = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
        try {
            login(db)
            var repo = RoomCompanionRepository(db)
            repo.saveProfile(" Mira ", " Shah ", "mira@example.test", "2000-02-29")
            repo.markRead(setOf("welcome", "offer:HELLO200"))
            repo.notificationSettings(false, true)
            repo.paymentPreset(demoPaymentPresets.first(), true)
            repo.sendMessage("support", "How do returns work?")
            assertFailsWith<IllegalArgumentException> { repo.paymentPreset("real-card", true) }
            assertFailsWith<IllegalArgumentException> { repo.saveProfile("Mira", "Shah", "bad email", "") }
            assertFailsWith<IllegalArgumentException> { repo.saveProfile("Mira", "Shah", "", "2001-02-29") }
            val saved = repo.observe().first()
            db.close(); db = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path)); repo = RoomCompanionRepository(db)
            assertEquals(saved, repo.observe().first())
            assertEquals("Mira", RoomAuthRepository(db).observe().first().profile!!.firstName)
            login(db, "9000000001")
            assertEquals(CompanionSnapshot(), repo.observe().first())
            login(db)
            assertEquals(saved, repo.observe().first())
            repo.paymentPreset(demoPaymentPresets.first(), false)
            assertTrue(repo.observe().first().paymentPresets.isEmpty())
        } finally { db.close(); folder.deleteRecursively() }
    }
    @Test fun returnsAndRatingsEnforceDeliveryWindowAndPreventDuplicateRequests(): Unit = runBlocking {
        val db = buildDatabase(Room.inMemoryDatabaseBuilder<ParcelDatabase>())
        var time = 1000L
        try {
            login(db)
            val commerce = RoomCommerceRepository(db) { time }
            commerce.initialize(); commerce.addToCart("sound-one", "Sand")
            val pending = commerce.prepareCheckout("order", "demo", time)
            commerce.finalizeCheckout(pending.id, pending.totals.total.minorUnits, "Local demo payment")
            val delivery = RoomDeliveryRepository(db) { time }; delivery.refresh()
            val repo = RoomCompanionRepository(db) { time }
            val request = ReturnRequest("order", 0, "Damaged")
            assertFailsWith<IllegalArgumentException> { repo.requestReturn(request) }
            assertFailsWith<IllegalStateException> { repo.rate("order", 5) }
            delivery.deliverNow("order")
            assertFailsWith<IllegalArgumentException> { repo.requestReturn(request.copy(exchangeSize = "M")) }
            assertFailsWith<IllegalStateException> { repo.requestReturn(request.copy(lineIndex = 99)) }
            repo.requestReturn(request)
            assertFailsWith<IllegalStateException> { repo.requestReturn(request) }
            repo.rate("order", 4); repo.requestCallback("order", "Return")
            assertEquals(4, repo.observe().first().ratings["order"])
            assertEquals(listOf(request), repo.observe().first().returns)
            time += 10 * DELIVERY_DAY_MS + 1
            assertFailsWith<IllegalArgumentException> { repo.requestReturn(request) }
        } finally { db.close() }
    }
    @Test fun versionFourMigrationKeepsTheSignedInProfileAndShopping(): Unit = runBlocking {
        val folder = createTempDirectory("parcel-companion-migration").toFile()
        val path = folder.resolve("shop.db").absolutePath
        val schema = Json.parseToJsonElement(java.io.File("schemas/com.parcel.data.ParcelDatabase/4.json").readText()).jsonObject.getValue("database").jsonObject
        val connection = BundledSQLiteDriver().open(path)
        fun sql(value: String) = connection.prepare(value).use { it.step() }
        schema.getValue("entities").jsonArray.forEach { value ->
            val row = value.jsonObject; val table = row.getValue("tableName").jsonPrimitive.content
            sql(row.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            row["indices"]?.jsonArray?.forEach { sql(it.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)) }
        }
        sql("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        sql("INSERT INTO room_master_table VALUES(42, '${schema.getValue("identityHash").jsonPrimitive.content}')")
        sql("INSERT INTO shopper_profiles VALUES('+919000000000', 'Existing', 'Shopper')")
        sql("INSERT INTO auth_session VALUES(1, '+919000000000')")
        sql("INSERT INTO saved_items VALUES('sound-one')")
        sql("PRAGMA user_version = 4"); connection.close()
        val db = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
        try {
            val repo = RoomCompanionRepository(db)
            repo.markRead(setOf("welcome"))
            assertEquals(setOf("welcome"), repo.observe().first().readNotifications)
            assertEquals("Existing", RoomAuthRepository(db).observe().first().profile!!.firstName)
            assertEquals(listOf("sound-one"), db.shoppingDao().observeSavedIds().first())
        } finally { db.close(); folder.deleteRecursively() }
    }
}
