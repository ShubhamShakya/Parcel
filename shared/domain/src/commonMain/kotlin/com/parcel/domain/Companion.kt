package com.parcel.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable data class ConversationLine(val text: String, val fromShopper: Boolean)
@Serializable data class ReturnRequest(val orderId: String, val lineIndex: Int, val reason: String, val exchangeSize: String? = null)
@Serializable data class CompanionSnapshot(
    val email: String = "", val birthday: String = "", val readNotifications: Set<String> = emptySet(),
    val showOffers: Boolean = true, val showOrders: Boolean = true,
    val paymentPresets: Set<String> = emptySet(),
    val conversations: Map<String, List<ConversationLine>> = emptyMap(),
    val returns: List<ReturnRequest> = emptyList(), val ratings: Map<String, Int> = emptyMap(),
    val callbacks: Map<String, String> = emptyMap(),
)
val demoPaymentPresets = listOf("Demo UPI · shopper@parcel.test", "Demo Visa · •••• 4242", "Demo Mastercard · •••• 4444")
val returnReasons = listOf("Too small", "Too large", "Damaged", "Different item", "Quality concern", "Changed my mind")
fun returnEligibility(progress: DeliveryProgress?, now: Long): String? = when {
    progress == null || progress.advance(now).stage != DeliveryStage.Delivered -> "Available after delivery."
    now > progress.milestoneTime(DeliveryStage.Delivered) + 10 * DELIVERY_DAY_MS -> "The 10-day return window has closed."
    else -> null
}
fun profileDetailsError(first: String, last: String, email: String, birthday: String): String? =
    nameError(first, last) ?: when {
        email.isNotBlank() && (!Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email) || email.length > 150) -> "Enter a valid email address or leave it blank."
        birthday.isNotBlank() && !validBirthday(birthday) -> "Use a valid past date in YYYY-MM-DD format."
        else -> null
    }
private fun validBirthday(value: String): Boolean {
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return false
    val (year, month, day) = value.split('-').map { it.toInt() }
    if (year < 1900 || month !in 1..12 || value > kotlin.time.Clock.System.now().toString().take(10)) return false
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    return day in 1..listOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)[month - 1]
}
interface CompanionRepository {
    fun observe(): Flow<CompanionSnapshot>
    suspend fun saveProfile(first: String, last: String, email: String, birthday: String)
    suspend fun markRead(ids: Set<String>)
    suspend fun notificationSettings(orders: Boolean, offers: Boolean)
    suspend fun paymentPreset(preset: String, saved: Boolean)
    suspend fun sendMessage(thread: String, text: String)
    suspend fun requestReturn(request: ReturnRequest)
    suspend fun rate(orderId: String, stars: Int)
    suspend fun requestCallback(orderId: String, issue: String)
}

/** Illustrative body measurements, never represented as a manufacturer's sizing data. */
data class DemoSizeMeasurement(val label: String, val chestCm: Double?, val waistCm: Double)
fun demoSizeMeasurements(clothing: ClothingDetails): List<DemoSizeMeasurement> = clothing.sizes.mapNotNull { size ->
    val numeric = size.toDoubleOrNull()
    if (numeric != null && numeric in 24.0..50.0) DemoSizeMeasurement(size, null, numeric * 2.54)
    else {
        val index = listOf("XS", "S", "M", "L", "XL", "XXL", "3XL").indexOf(size.uppercase())
        if (index < 0) null else DemoSizeMeasurement(size, 80.0 + index * 6, 64.0 + index * 6)
    }
}
fun suggestedDemoSize(measurements: List<DemoSizeMeasurement>, waistCm: Double): String? =
    measurements.sortedBy { it.waistCm }.firstOrNull { waistCm > 0 && it.waistCm >= waistCm }?.label
