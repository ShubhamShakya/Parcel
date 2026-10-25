package com.parcel.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

const val DELIVERY_DAY_MS = 24 * 60 * 60 * 1000L
const val DELIVERY_DURATION_MS = 4 * 60 * 60 * 1000L
const val SHELF_SIZE = 12

enum class DeliveryStage(val label: String, val afterHours: Int) {
    Confirmed("Order confirmed", 0), Shipped("Shipped", 1), NearbyHub("Reached your nearby hub", 2),
    OutForDelivery("Out for delivery", 3), Delivered("Delivered", 4)
}

@Serializable
data class DeliveryProgress(val orderId: String, val confirmedAt: Long, val otp: String,
    val observedAt: Long = confirmedAt, val instantDeliveredAt: Long? = null, val unboxedAt: Long? = null,
    val scheduleDurationMs: Long = DELIVERY_DURATION_MS) {
    val dueAt: Long get() = confirmedAt + scheduleDurationMs
    val stage: DeliveryStage get() = if (instantDeliveredAt != null) DeliveryStage.Delivered else
        DeliveryStage.entries.last { observedAt >= scheduledTime(it) }
    fun advance(now: Long) = copy(observedAt = maxOf(observedAt, confirmedAt, now))
    fun deliverNow(now: Long): DeliveryProgress = advance(now).let {
        if (it.stage == DeliveryStage.Delivered) it else it.copy(instantDeliveredAt = it.observedAt)
    }
    fun unbox(now: Long): DeliveryProgress = advance(now).let {
        check(it.stage == DeliveryStage.Delivered) { "Your parcel needs to arrive before you can open it." }
        if (it.unboxedAt != null) it else it.copy(unboxedAt = it.observedAt)
    }
    fun scheduledTime(stage: DeliveryStage): Long = confirmedAt + scheduleDurationMs * stage.ordinal / 4
    fun milestoneTime(stage: DeliveryStage): Long = minOf(scheduledTime(stage), instantDeliveredAt ?: Long.MAX_VALUE)
}

data class PurchasedItem(val id: String, val orderId: String, val productId: String, val title: String,
    val variant: String, val paid: Money)
/** Allocate the frozen paid total proportionally across individual units, retaining every paise. */
fun purchasedItems(receipt: Receipt): List<PurchasedItem> {
    val checkout = receipt.checkout
    val subtotal = checkout.lines.sumOf { it.unitPrice.minorUnits * it.quantity }
    var cumulative = 0L
    var allocated = 0L
    return checkout.lines.flatMapIndexed { lineIndex, line ->
        (0 until line.quantity).map { unit ->
            cumulative += line.unitPrice.minorUnits
            val next = if (subtotal == 0L) 0L else checkout.totals.total.minorUnits * cumulative / subtotal
            val paid = next - allocated
            allocated = next
            PurchasedItem("${checkout.id}:$lineIndex:$unit", checkout.id, line.productId, line.title, line.variant, Money(paid))
        }
    }
}

data class ShelfPlacement(val slot: Int, val itemId: String)
data class DeliverySnapshot(val orders: List<DeliveryProgress> = emptyList(), val shelf: List<ShelfPlacement> = emptyList())
interface DeliveryRepository {
    fun observe(): Flow<DeliverySnapshot>
    suspend fun refresh()
    suspend fun deliverNow(orderId: String)
    suspend fun unbox(orderId: String)
    suspend fun place(slot: Int, itemId: String)
    suspend fun clearSlot(slot: Int)
}

/** The chosen schedule is stored with the order, independent of future clock/timezone changes. */
fun scheduledDelivery(orderId: String, confirmedAt: Long, otp: String, finishBeforeMidnight: Boolean,
    nextMidnight: Long): DeliveryProgress {
    val duration = if (finishBeforeMidnight)
        minOf(DELIVERY_DURATION_MS, (nextMidnight - confirmedAt - 1).coerceAtLeast(0))
    else DELIVERY_DURATION_MS
    return DeliveryProgress(orderId, confirmedAt, otp, scheduleDurationMs = duration)
}
