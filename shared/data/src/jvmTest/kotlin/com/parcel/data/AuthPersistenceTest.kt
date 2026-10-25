package com.parcel.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.parcel.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class AuthPersistenceTest {
    @Test fun registrationAndReturningLoginPersistAcrossDatabaseReopen(): Unit = runBlocking {
        val folder = createTempDirectory("parcel-auth").toFile()
        val path = folder.resolve("shop.db").absolutePath
        fun open() = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
        var db = open()
        try {
            var repo = RoomAuthRepository(db) { 1000 }
            repo.requestOtp("GB", "123456789")
            assertFailsWith<IllegalStateException> { repo.register("Mira", "Shah") }
            repo.verifyOtp("aB9!")
            assertTrue(repo.observe().first { it.challenge?.verified == true }.challenge!!.verified)
            assertNull(repo.observe().first().profile)
            db.close(); db = open(); repo = RoomAuthRepository(db) { 2000 }
            assertTrue(repo.observe().first().challenge!!.verified)
            repo.register(" Mira ", " Shah ")
            val profile = repo.observe().first { it.profile != null }.profile!!
            assertEquals(ShopperProfile("+44123456789", "Mira", "Shah"), profile)
            db.close(); db = open(); repo = RoomAuthRepository(db) { 3000 }
            assertEquals(profile, repo.observe().first().profile)
            repo.signOut(); assertNull(repo.observe().first().profile)
            repo.requestOtp("GB", "123456789"); repo.verifyOtp("0000")
            assertEquals(profile, repo.observe().first { it.profile != null }.profile)
            assertNull(repo.observe().first().challenge)
            repo.signOut(); repo.requestOtp("IN", "123456789"); repo.verifyOtp("9876")
            assertNull(repo.observe().first().profile) // Different dial code is a different number.
            repo.register("Aarav", "Mehta")
            assertEquals("Aarav", repo.observe().first { it.profile != null }.profile!!.firstName)
        } finally { db.close(); folder.deleteRecursively() }
    }
    @Test fun resendWaitsThirtySecondsAndRestartsOnlyWhenRequested(): Unit = runBlocking {
        val db = buildDatabase(Room.inMemoryDatabaseBuilder<ParcelDatabase>())
        var now = 1000L
        try {
            val repo = RoomAuthRepository(db) { now }
            repo.requestOtp("IN", "9000000000")
            assertEquals(31_000, repo.observe().first().challenge!!.resendAt)
            now = 30_999
            assertFailsWith<IllegalStateException> { repo.resendOtp() }
            repo.requestOtp("IN", "9000000000")
            assertEquals(31_000, repo.observe().first().challenge!!.resendAt)
            now = 31_000; repo.resendOtp()
            assertEquals(61_000, repo.observe().first().challenge!!.resendAt)
            assertFailsWith<IllegalArgumentException> { repo.verifyOtp("123") }
            assertFailsWith<IllegalArgumentException> { repo.verifyOtp("12345") }
            assertFalse(repo.observe().first().challenge!!.verified)
            repo.verifyOtp("1111")
            assertFailsWith<IllegalArgumentException> { repo.register("", "Last") }
            assertNull(repo.observe().first().profile)
        } finally { db.close() }
    }
    @Test fun versionThreeMigrationPreservesShelfAndAllowsLogin(): Unit = runBlocking {
        val folder = createTempDirectory("parcel-auth-migration").toFile()
        val path = folder.resolve("shop.db").absolutePath
        val schema = Json.parseToJsonElement(java.io.File("schemas/com.parcel.data.ParcelDatabase/3.json").readText()).jsonObject.getValue("database").jsonObject
        val c = BundledSQLiteDriver().open(path)
        fun sql(value: String) = c.prepare(value).use { it.step() }
        schema.getValue("entities").jsonArray.forEach { value ->
            val row = value.jsonObject; val table = row.getValue("tableName").jsonPrimitive.content
            sql(row.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            row["indices"]?.jsonArray?.forEach { sql(it.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)) }
        }
        sql("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        sql("INSERT INTO room_master_table VALUES(42, '${schema.getValue("identityHash").jsonPrimitive.content}')")
        sql("INSERT INTO shelf_placements VALUES(7, 'old-order:0:0')")
        sql("INSERT INTO saved_items VALUES('sound-one')")
        sql("PRAGMA user_version = 3"); c.close()
        val db = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
        try {
            val repo = RoomAuthRepository(db) { 1000 }
            assertNull(repo.observe().first().profile)
            repo.requestOtp("IN", "9000000000"); repo.verifyOtp("9999"); repo.register("Test", "Shopper")
            assertNotNull(repo.observe().first { it.profile != null }.profile)
            assertEquals("old-order:0:0", db.deliveryDao().observeShelf().first().single().itemId)
            assertEquals(listOf("sound-one"), db.shoppingDao().observeSavedIds().first())
        } finally { db.close(); folder.deleteRecursively() }
    }
}
