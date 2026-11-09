package com.shubhamshakya.parcel.data

import androidx.room.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.shubhamshakya.parcel.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Entity(tableName = "saved_items")
data class SavedItemEntity(@PrimaryKey val productId: String)
@Entity(tableName = "preferences")
data class PreferenceEntity(@PrimaryKey val key: String, val value: Boolean)

@Dao
interface ShoppingDao {
    @Query("SELECT value FROM preferences WHERE `key` = 'dark_mode'")
    fun observeDarkMode(): Flow<Boolean?>
    @Query("SELECT value FROM preferences WHERE `key` = 'appearance_system'")
    fun observeFollowSystem(): Flow<Boolean?>
    @Query("SELECT productId FROM saved_items ORDER BY productId")
    fun observeSavedIds(): Flow<List<String>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSaved(item: SavedItemEntity)
    @Query("DELETE FROM saved_items WHERE productId = :productId")
    suspend fun deleteSaved(productId: String)
    @Query("SELECT value FROM preferences WHERE `key` = 'disclosure_v1'")
    fun observeDisclosure(): Flow<Boolean?>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(preference: PreferenceEntity)
}

@Database(entities = [SavedItemEntity::class, PreferenceEntity::class, CartEntity::class,
    AddressEntity::class, CheckoutSettingEntity::class, PendingCheckoutEntity::class, ReceiptEntity::class, DeliveryEntity::class, ShelfEntity::class, ShopperEntity::class, AuthSessionEntity::class, OtpEntity::class, CompanionEntity::class],
    version = 5, exportSchema = true, autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5)])
@ConstructedBy(ParcelDatabaseConstructor::class)
abstract class ParcelDatabase : RoomDatabase() { abstract fun shoppingDao(): ShoppingDao; abstract fun commerceDao(): CommerceDao; abstract fun deliveryDao(): DeliveryDao; abstract fun authDao(): AuthDao; abstract fun companionDao(): CompanionDao }

@Suppress("KotlinNoActualForExpect")
expect object ParcelDatabaseConstructor : RoomDatabaseConstructor<ParcelDatabase> {
    override fun initialize(): ParcelDatabase
}

fun buildDatabase(builder: RoomDatabase.Builder<ParcelDatabase>): ParcelDatabase = builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()

class RoomShoppingRepository(private val dao: ShoppingDao) : SavedItemsRepository, PreferencesRepository, AppearanceRepository {
    // Two existing boolean preferences express the three modes, so no schema change is needed.
    override fun observeAppearance(): Flow<AppearanceMode> =
        combine(dao.observeFollowSystem(), dao.observeDarkMode()) { system, dark ->
            when {
                system ?: true -> AppearanceMode.System
                dark ?: true -> AppearanceMode.Dark
                else -> AppearanceMode.Light
            }
        }
    override suspend fun setAppearance(mode: AppearanceMode) {
        if (mode != AppearanceMode.System) dao.setPreference(PreferenceEntity("dark_mode", mode == AppearanceMode.Dark))
        dao.setPreference(PreferenceEntity("appearance_system", mode == AppearanceMode.System))
    }
    override fun observeSavedIds(): Flow<Set<String>> = dao.observeSavedIds().map { it.toSet() }
    override suspend fun setSaved(productId: String, saved: Boolean) {
        require(MockCatalogRepository.products.any { it.id == productId }) { "Unknown product" }
        if (saved) dao.insertSaved(SavedItemEntity(productId)) else dao.deleteSaved(productId)
    }
    override fun observeDisclosureAccepted() = dao.observeDisclosure().map { it == true }
    override suspend fun acceptDisclosure() = dao.setPreference(PreferenceEntity("disclosure_v1", true))
}
