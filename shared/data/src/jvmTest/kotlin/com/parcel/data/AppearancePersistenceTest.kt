package com.parcel.data

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class AppearancePersistenceTest {
    @Test fun defaultsToDarkAndRestoresAnExplicitLightPreference(): Unit = runBlocking {
        val folder = createTempDirectory("parcel-appearance").toFile()
        val path = folder.resolve("shop.db").absolutePath
        var db = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
        try {
            val repo = RoomShoppingRepository(db.shoppingDao())
            assertTrue(repo.observeDarkMode().first())
            repo.setDarkMode(false)
            db.close(); db = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
            val restored = RoomShoppingRepository(db.shoppingDao())
            assertFalse(restored.observeDarkMode().first())
            restored.setDarkMode(true)
            assertTrue(restored.observeDarkMode().first())
        } finally { db.close(); folder.deleteRecursively() }
    }
}
