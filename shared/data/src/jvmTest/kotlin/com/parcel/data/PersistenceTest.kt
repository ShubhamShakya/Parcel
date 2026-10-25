package com.parcel.data

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.test.*

class PersistenceTest {
    @Test fun wishlistAndDisclosureSurviveDatabaseReopen() = runBlocking {
        val directory = createTempDirectory("parcel-db-test").toFile()
        val path = directory.resolve("parcel.db").absolutePath
        fun open() = buildDatabase(Room.databaseBuilder<ParcelDatabase>(path))
        try {
            val first = open()
            try {
                val repo = RoomShoppingRepository(first.shoppingDao())
                assertFalse(repo.observeDisclosureAccepted().first())
                repo.acceptDisclosure()
                repo.setSaved("sound-one", true)
                repo.setSaved("sound-one", true)
                assertEquals(setOf("sound-one"), repo.observeSavedIds().first())
            } finally { first.close() }
            val reopened = open()
            try {
                val repo = RoomShoppingRepository(reopened.shoppingDao())
                assertTrue(repo.observeDisclosureAccepted().first())
                assertEquals(setOf("sound-one"), repo.observeSavedIds().first())
                repo.setSaved("sound-one", false)
                assertTrue(repo.observeSavedIds().first().isEmpty())
                assertFailsWith<IllegalArgumentException> { repo.setSaved("unknown-id", true) }
            } finally { reopened.close() }
        } finally { directory.deleteRecursively() }
        Unit
    }
}
