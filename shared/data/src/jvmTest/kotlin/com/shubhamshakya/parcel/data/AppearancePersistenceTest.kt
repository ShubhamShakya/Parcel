package com.shubhamshakya.parcel.data

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import com.shubhamshakya.parcel.domain.AppearanceMode
import kotlin.test.*

class AppearancePersistenceTest {
    @Test fun defaultsToSystemAndRestoresAnExplicitChoice(): Unit = runBlocking {
        val folder = createTempDirectory("parcel-appearance").toFile()
        val path = folder.resolve("shop.db").absolutePath
        var db = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
        try {
            val repo = RoomShoppingRepository(db.shoppingDao())
            assertEquals(AppearanceMode.System, repo.observeAppearance().first())
            repo.setAppearance(AppearanceMode.Light)
            db.close(); db = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
            val restored = RoomShoppingRepository(db.shoppingDao())
            assertEquals(AppearanceMode.Light, restored.observeAppearance().first())
            restored.setAppearance(AppearanceMode.Dark)
            assertEquals(AppearanceMode.Dark, restored.observeAppearance().first())
            restored.setAppearance(AppearanceMode.System)
            assertEquals(AppearanceMode.System, restored.observeAppearance().first())
        } finally { db.close(); folder.deleteRecursively() }
    }
}
