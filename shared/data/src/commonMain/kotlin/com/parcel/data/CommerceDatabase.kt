package com.parcel.data

import androidx.room.*
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.parcel.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

@Entity(tableName = "cart", primaryKeys = ["productId", "variant"])
data class CartEntity(val productId: String, val variant: String, val quantity: Int)
@Entity(tableName = "addresses")
data class AddressEntity(@PrimaryKey val id: String, val label: String, val recipient: String,
    val line: String, val locality: String, val postalCode: String)
@Entity(tableName = "checkout_settings")
data class CheckoutSettingEntity(@PrimaryKey val id: Int = 1, val seeded: Boolean = false,
    val selectedAddressId: String? = null, val offerCode: String? = null)
@Entity(tableName = "pending_checkout")
data class PendingCheckoutEntity(@PrimaryKey val singleton: Int = 1, val payload: String)
@Entity(tableName = "receipts")
data class ReceiptEntity(@PrimaryKey val id: String, val createdAt: Long, val payload: String)

@Dao
interface CommerceDao {
    @Query("SELECT * FROM cart ORDER BY productId, variant") fun observeCart(): Flow<List<CartEntity>>
    @Query("SELECT * FROM addresses ORDER BY id") fun observeAddresses(): Flow<List<AddressEntity>>
    @Query("SELECT * FROM checkout_settings WHERE id = 1") fun observeSettings(): Flow<CheckoutSettingEntity?>
    @Query("SELECT * FROM pending_checkout WHERE singleton = 1") fun observePending(): Flow<PendingCheckoutEntity?>
    @Query("SELECT * FROM receipts ORDER BY createdAt DESC, id DESC") fun observeReceipts(): Flow<List<ReceiptEntity>>
    @Query("SELECT * FROM cart ORDER BY productId, variant") suspend fun cart(): List<CartEntity>
    @Query("SELECT * FROM addresses ORDER BY id") suspend fun addresses(): List<AddressEntity>
    @Query("SELECT * FROM checkout_settings WHERE id = 1") suspend fun settings(): CheckoutSettingEntity?
    @Query("SELECT * FROM pending_checkout WHERE singleton = 1") suspend fun pending(): PendingCheckoutEntity?
    @Query("SELECT * FROM receipts WHERE id = :id") suspend fun receipt(id: String): ReceiptEntity?
    @Upsert suspend fun putCart(row: CartEntity)
    @Query("DELETE FROM cart WHERE productId = :id AND variant = :variant") suspend fun removeCart(id: String, variant: String)
    @Upsert suspend fun putAddress(row: AddressEntity)
    @Query("DELETE FROM addresses WHERE id = :id") suspend fun removeAddress(id: String)
    @Upsert suspend fun putSettings(row: CheckoutSettingEntity)
    @Upsert suspend fun putPending(row: PendingCheckoutEntity)
    @Query("DELETE FROM pending_checkout") suspend fun clearPending()
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun putReceipt(row: ReceiptEntity)
}

private fun AddressEntity.domain() = DemoAddress(id, label, recipient, line, locality, postalCode)
private fun DemoAddress.entity() = AddressEntity(id, label, recipient, line, locality, postalCode)

class RoomCommerceRepository(private val db: ParcelDatabase, private val now: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() }) : CommerceRepository {
    private val dao = db.commerceDao()
    private val json = Json { ignoreUnknownKeys = true }
    override fun observe(): Flow<CommerceSnapshot> = combine(dao.observeCart(), dao.observeAddresses(),
        dao.observeSettings(), dao.observePending(), dao.observeReceipts()) { cart, addresses, settings, pending, receipts ->
        CommerceSnapshot(cart.map { CartItem(it.productId, it.variant, it.quantity) }, addresses.map { it.domain() },
            settings?.selectedAddressId, settings?.offerCode,
            pending?.let { json.decodeFromString<PendingCheckout>(it.payload) },
            receipts.map { json.decodeFromString<Receipt>(it.payload) })
    }
    private suspend fun <T> transaction(block: suspend () -> T): T = db.useWriterConnection { it.immediateTransaction { block() } }
    private suspend fun editable() { check(dao.pending() == null) { "Resolve the pending checkout before changing your cart or address." } }
    override suspend fun initialize() = transaction {
        val settings = dao.settings() ?: CheckoutSettingEntity()
        if (!settings.seeded) {
            demoAddresses.forEach { dao.putAddress(it.entity()) }
            dao.putSettings(settings.copy(seeded = true, selectedAddressId = demoAddresses.first().id))
        }
    }
    override suspend fun addToCart(productId: String, variant: String) = transaction {
        editable()
        val p = MockCatalogRepository.products.first { it.id == productId }
        require(variant in p.variants)
        val quantity = (dao.cart().find { it.productId == productId && it.variant == variant }?.quantity ?: 0) + 1
        require(quantity <= 10) { "You can add up to 10 of each variant." }
        dao.putCart(CartEntity(productId, variant, quantity))
    }
    override suspend fun setQuantity(productId: String, variant: String, quantity: Int) = transaction {
        editable(); require(quantity in 0..10)
        require(MockCatalogRepository.products.any { it.id == productId && variant in it.variants })
        if (quantity == 0) dao.removeCart(productId, variant) else dao.putCart(CartEntity(productId, variant, quantity))
    }
    override suspend fun saveAddress(address: DemoAddress) = transaction {
        editable(); require(address.id.isNotBlank()); require(address.validationError() == null) { address.validationError().orEmpty() }
        dao.putAddress(address.entity())
        val settings = dao.settings() ?: CheckoutSettingEntity(seeded = true)
        if (settings.selectedAddressId == null) dao.putSettings(settings.copy(selectedAddressId = address.id))
    }
    override suspend fun deleteAddress(id: String) = transaction {
        editable(); dao.removeAddress(id)
        val settings = dao.settings() ?: CheckoutSettingEntity(seeded = true)
        if (settings.selectedAddressId == id) dao.putSettings(settings.copy(selectedAddressId = dao.addresses().firstOrNull()?.id))
    }
    override suspend fun selectAddress(id: String) = transaction {
        editable(); require(dao.addresses().any { it.id == id })
        dao.putSettings((dao.settings() ?: CheckoutSettingEntity(seeded = true)).copy(selectedAddressId = id))
    }
    override suspend fun selectOffer(code: String?) = transaction {
        editable(); require(code == null || shoppingOffers.any { it.code == code })
        dao.putSettings((dao.settings() ?: CheckoutSettingEntity(seeded = true)).copy(offerCode = code))
    }
    override suspend fun prepareCheckout(id: String, mode: String, now: Long): PendingCheckout = transaction {
        require(mode in listOf("demo", "payu")); require(id.isNotBlank())
        dao.pending()?.let {
            val pending = json.decodeFromString<PendingCheckout>(it.payload)
            check(pending.mode == mode) { "Finish the existing checkout first." }; return@transaction pending
        }
        val settings = dao.settings() ?: error("Select a demo address.")
        val address = dao.addresses().firstOrNull { it.id == settings.selectedAddressId }?.domain() ?: error("Select a demo address.")
        val lines = cartLines(dao.cart().map { CartItem(it.productId, it.variant, it.quantity) }, MockCatalogRepository.products)
        check(lines.isNotEmpty()) { "Your cart is empty." }
        val pending = PendingCheckout(id, mode, lines, address, calculateTotals(lines, settings.offerCode), now)
        dao.putPending(PendingCheckoutEntity(payload = json.encodeToString(pending))); pending
    }
    override suspend fun finalizeCheckout(id: String, verifiedTotal: Long, paymentLabel: String): Receipt = transaction {
        dao.receipt(id)?.let { return@transaction json.decodeFromString<Receipt>(it.payload) }
        val pending = dao.pending()?.let { json.decodeFromString<PendingCheckout>(it.payload) } ?: error("Checkout not found.")
        require(pending.id == id && pending.totals.total.minorUnits == verifiedTotal) { "Payment does not match this checkout." }
        require(paymentLabel == if (pending.mode == "demo") "Local demo payment" else "PayU sandbox · verified")
        val receipt = Receipt(pending, paymentLabel)
        dao.putReceipt(ReceiptEntity(id, pending.createdAt, json.encodeToString(receipt)))
        val progress = newDeliveryProgress(id, now())
        db.deliveryDao().putOrder(DeliveryEntity(id, json.encodeToString(progress)))
        pending.lines.forEach { dao.removeCart(it.productId, it.variant) }
        dao.clearPending(); receipt
    }
    override suspend fun abandonCheckout(id: String) = transaction {
        val pending = dao.pending()?.let { json.decodeFromString<PendingCheckout>(it.payload) }
        if (pending?.id == id) dao.clearPending()
    }
}
