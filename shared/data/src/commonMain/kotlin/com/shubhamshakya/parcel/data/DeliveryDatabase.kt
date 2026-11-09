package com.shubhamshakya.parcel.data

import androidx.room.*
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.shubhamshakya.parcel.domain.*
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@Entity(tableName = "delivery_progress")
data class DeliveryEntity(@PrimaryKey val orderId: String, val payload: String)
@Entity(tableName = "shelf_placements", indices = [Index(value = ["itemId"], unique = true)])
data class ShelfEntity(@PrimaryKey val slot: Int, val itemId: String)
@Dao
interface DeliveryDao {
    @Query("SELECT * FROM preferences WHERE `key` LIKE 'shelf_unlocked_%' AND value = 1")
    fun observeUnlocks(): kotlinx.coroutines.flow.Flow<List<PreferenceEntity>>
    @Query("SELECT value FROM preferences WHERE `key` = :key") suspend fun preference(key: String): Boolean?
    @Query("SELECT * FROM shelf_placements") suspend fun shelf(): List<ShelfEntity>

    @Query("SELECT * FROM delivery_progress") fun observeOrders(): kotlinx.coroutines.flow.Flow<List<DeliveryEntity>>
    @Query("SELECT * FROM shelf_placements ORDER BY slot") fun observeShelf(): kotlinx.coroutines.flow.Flow<List<ShelfEntity>>
    @Query("SELECT * FROM delivery_progress WHERE orderId = :id") suspend fun order(id: String): DeliveryEntity?
    @Query("SELECT * FROM receipts ORDER BY createdAt DESC") suspend fun receipts(): List<ReceiptEntity>
    @Upsert suspend fun putOrder(order: DeliveryEntity)
    @Upsert suspend fun putSlot(slot: ShelfEntity)
    @Query("DELETE FROM shelf_placements WHERE slot = :slot OR itemId = :itemId") suspend fun freePlacement(slot: Int, itemId: String)
    @Query("DELETE FROM shelf_placements WHERE slot = :slot") suspend fun clearSlot(slot: Int)
}

class RoomDeliveryRepository(private val db: ParcelDatabase, private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }) : DeliveryRepository {
    private val dao = db.deliveryDao()
    private val json = Json { ignoreUnknownKeys = true }
    override fun observe() = combine(dao.observeOrders(), dao.observeShelf(), dao.observeUnlocks()) { orders, shelf, unlocks ->
        DeliverySnapshot(orders.map { json.decodeFromString<DeliveryProgress>(it.payload) }, shelf.map { ShelfPlacement(it.slot, it.itemId) },
            (0 until FREE_SHELF_SIZE).toSet() + unlocks.mapNotNull { it.key.removePrefix("shelf_unlocked_").toIntOrNull()?.takeIf { slot -> slot in 0 until SHELF_SIZE } })
    }
    private suspend fun <T> transaction(block: suspend () -> T): T = db.useWriterConnection { it.immediateTransaction { block() } }
    private suspend fun progress(id: String): DeliveryProgress {
        dao.order(id)?.let { return json.decodeFromString(it.payload) }
        val receipt = db.commerceDao().receipt(id) ?: error("Order not found.")
        // Receipts from versions before tracking retain their original order date.
        return newDeliveryProgress(id, receipt.createdAt)
    }
    private suspend fun save(progress: DeliveryProgress) = dao.putOrder(DeliveryEntity(progress.orderId, json.encodeToString(progress)))
    override suspend fun refresh() = transaction {
        if (dao.preference("shelf_access_initialized") != true) {
            // Preserve existing occupied spaces on upgrade, even beyond the free six.
            dao.shelf().filter { it.slot >= FREE_SHELF_SIZE }.forEach {
                db.shoppingDao().setPreference(PreferenceEntity("shelf_unlocked_${it.slot}", true))
            }
            db.shoppingDao().setPreference(PreferenceEntity("shelf_access_initialized", true))
        }
        val time = now()
        dao.receipts().forEach { receipt ->
            val original = progress(receipt.id)
            val updated = original.advance(time)
            if (updated != original || dao.order(receipt.id) == null) save(updated)
        }
    }
    override suspend fun deliverNow(orderId: String) = transaction { save(progress(orderId).deliverNow(now())) }
    override suspend fun unbox(orderId: String) = transaction { save(progress(orderId).unbox(now())) }
    override suspend fun place(slot: Int, itemId: String) = transaction {
        require(slot in 0 until SHELF_SIZE)
        check(slot < FREE_SHELF_SIZE || dao.preference("shelf_unlocked_$slot") == true) { "Watch an ad to unlock this space first." }
        val owned = dao.receipts().any { row ->
            val receipt = json.decodeFromString<Receipt>(row.payload)
            progress(row.id).unboxedAt != null && purchasedItems(receipt).any { it.id == itemId }
        }
        check(owned) { "Unbox this item before putting it on your shelf." }
        dao.freePlacement(slot, itemId)
        dao.putSlot(ShelfEntity(slot, itemId))
    }
    override suspend fun unlockSlot(slot: Int) = transaction {
        require(slot in FREE_SHELF_SIZE until SHELF_SIZE)
        db.shoppingDao().setPreference(PreferenceEntity("shelf_unlocked_$slot", true))
    }
    override suspend fun clearSlot(slot: Int) = transaction { require(slot in 0 until SHELF_SIZE); dao.clearSlot(slot) }
}
