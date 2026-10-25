package com.parcel.data

import androidx.room.*
import com.parcel.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@Entity(tableName = "companion_profiles")
data class CompanionEntity(@PrimaryKey val phone: String, val payload: String)
@Dao interface CompanionDao {
    @Query("SELECT c.* FROM companion_profiles c INNER JOIN auth_session s ON s.phone = c.phone WHERE s.id = 1") fun observe(): Flow<CompanionEntity?>
    @Query("SELECT phone FROM auth_session WHERE id = 1") suspend fun phone(): String?
    @Query("SELECT * FROM companion_profiles WHERE phone = :phone") suspend fun get(phone: String): CompanionEntity?
    @Upsert suspend fun put(entity: CompanionEntity)
    @Query("UPDATE shopper_profiles SET firstName = :first, lastName = :last WHERE phone = :phone") suspend fun rename(phone: String, first: String, last: String)
}
class RoomCompanionRepository(private val db: ParcelDatabase, private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }) : CompanionRepository {
    private val dao = db.companionDao()
    private val json = Json { ignoreUnknownKeys = true }
    override fun observe() = dao.observe().map { it?.let { json.decodeFromString<CompanionSnapshot>(it.payload) } ?: CompanionSnapshot() }
    private suspend fun mutate(change: suspend (String, CompanionSnapshot) -> CompanionSnapshot) {
        db.useWriterConnection { connection -> connection.immediateTransaction {
            val phone = dao.phone() ?: error("Sign in to continue.")
            val state = dao.get(phone)?.let { json.decodeFromString<CompanionSnapshot>(it.payload) } ?: CompanionSnapshot()
            dao.put(CompanionEntity(phone, json.encodeToString(change(phone, state))))
        } }
    }
    override suspend fun saveProfile(first: String, last: String, email: String, birthday: String) = mutate { phone, state ->
        require(profileDetailsError(first, last, email.trim(), birthday.trim()) == null) { profileDetailsError(first, last, email.trim(), birthday.trim()).orEmpty() }
        dao.rename(phone, first.trim(), last.trim()); state.copy(email = email.trim(), birthday = birthday.trim())
    }
    override suspend fun markRead(ids: Set<String>) = mutate { _, s -> s.copy(readNotifications = s.readNotifications + ids) }
    override suspend fun notificationSettings(orders: Boolean, offers: Boolean) = mutate { _, s -> s.copy(showOrders = orders, showOffers = offers) }
    override suspend fun paymentPreset(preset: String, saved: Boolean) = mutate { _, s ->
        require(preset in demoPaymentPresets); s.copy(paymentPresets = if (saved) s.paymentPresets + preset else s.paymentPresets - preset)
    }
    private suspend fun receipt(id: String): Receipt = db.commerceDao().receipt(id)?.let { json.decodeFromString(it.payload) } ?: error("Order not found.")
    private suspend fun progress(id: String): DeliveryProgress? = db.deliveryDao().order(id)?.let { json.decodeFromString<DeliveryProgress>(it.payload).advance(now()) }
    override suspend fun sendMessage(thread: String, text: String) = mutate { _, s ->
        require(text.isNotBlank() && text.length <= 500) { "Write a message of up to 500 characters." }
        if (thread != "support") receipt(thread.removePrefix("rider:"))
        val reply = if (thread == "support") when {
            text.contains("return", true) -> "Open an order, then Return or exchange. Requests are available for 10 days after simulated delivery."
            text.contains("pay", true) -> "Local checkout never charges you. PayU is test-only and requires sandbox configuration."
            else -> "I’m Parcel’s offline assistant. Choose an order under Help with an order to save an issue, or browse the help centre for answers."
        } else "This is a simulated delivery conversation. Your tracking page shows the current stage; you can also use Deliver it instantly. No real rider receives these messages."
        val lines = s.conversations[thread].orEmpty() + listOf(ConversationLine(text.trim(), true), ConversationLine(reply, false))
        s.copy(conversations = s.conversations + (thread to lines.takeLast(100)))
    }
    override suspend fun requestReturn(request: ReturnRequest) = mutate { _, s ->
        val order = receipt(request.orderId)
        require(returnEligibility(progress(request.orderId), now()) == null) { returnEligibility(progress(request.orderId), now()).orEmpty() }
        require(request.reason in returnReasons)
        val line = order.checkout.lines.getOrNull(request.lineIndex) ?: error("Choose an item.")
        val product = MockCatalogRepository.products.firstOrNull { it.id == line.productId }
        check(product?.clothing?.type?.group != ClothingGroup.Innerwear) { "This item is not eligible for returns." }
        request.exchangeSize?.let { require(it in product?.clothing?.sizes.orEmpty()) { "Choose an available exchange size." } }
        check(s.returns.none { it.orderId == request.orderId && it.lineIndex == request.lineIndex }) { "A request already exists for this item." }
        s.copy(returns = s.returns + request)
    }
    override suspend fun rate(orderId: String, stars: Int) = mutate { _, s ->
        receipt(orderId); require(stars in 1..5); check(progress(orderId)?.stage == DeliveryStage.Delivered) { "Rate your delivery after it arrives." }
        s.copy(ratings = s.ratings + (orderId to stars))
    }
    override suspend fun requestCallback(orderId: String, issue: String) = mutate { _, s ->
        receipt(orderId); require(issue in listOf("Delivery", "Payment", "Missing item", "Return")); s.copy(callbacks = s.callbacks + (orderId to issue))
    }
}
