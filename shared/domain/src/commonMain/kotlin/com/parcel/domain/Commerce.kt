package com.parcel.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class CartItem(val productId: String, val variant: String, val quantity: Int) {
    init { require(quantity in 1..10); require(productId.isNotBlank()); require(variant.isNotBlank()) }
    val key: String get() = "$productId|$variant"
}
@Serializable
data class DemoAddress(val id: String, val label: String, val recipient: String, val line: String,
    val locality: String, val postalCode: String) {
    fun validationError(): String? = when {
        label.isBlank() || recipient.isBlank() || line.isBlank() || locality.isBlank() -> "Complete all address fields."
        listOf(label, recipient, line, locality).any { it.length > 150 } -> "Keep each field under 150 characters."
        !postalCode.matches(Regex("110[0-9]{3}")) -> "Use a demo Delhi postal code beginning with 110."
        else -> null
    }
    val display: String get() = "$line, $locality, New Delhi $postalCode"
}
val demoAddresses = listOf(
    DemoAddress("demo-cp", "Connaught Place", "Demo shopper", "Demo Suite 12, Block A, Inner Circle", "Connaught Place", "110001"),
    DemoAddress("demo-barakhamba", "Barakhamba Road", "Demo shopper", "Demo Studio 8, Near Barakhamba Road Metro", "Barakhamba Road", "110001"),
    DemoAddress("demo-bengali", "Bengali Market", "Demo shopper", "Demo Flat 21, Near Bengali Market", "Bengali Market", "110001"),
)
@Serializable
data class Offer(val code: String, val title: String, val description: String, val minimum: Long,
    val percent: Int = 0, val fixedDiscount: Long = 0, val cap: Long = Long.MAX_VALUE, val waiveFee: Boolean = false)
val shoppingOffers = listOf(
    Offer("EVERYDAY10", "A little extra joy", "10% off · Up to ₹500 · Minimum ₹499", 49900, percent = 10, cap = 50000),
    Offer("BIGFINDS15", "Bigger finds, better savings", "15% off · Up to ₹750 · Minimum ₹1,999", 199900, percent = 15, cap = 75000),
    Offer("HELLO200", "₹200 off your next find", "Flat ₹200 off · Minimum ₹1,499", 149900, fixedDiscount = 20000),
    Offer("SMALLJOY100", "Small joys add up", "Flat ₹100 off · Minimum ₹999", 99900, fixedDiscount = 10000),
    Offer("FEEFREE", "Make it fee-free", "Convenience fee waived · Minimum ₹799", 79900, waiveFee = true),
)
fun randomOfferDeck(random: Random = Random.Default): List<Offer> = shoppingOffers.shuffled(random).take(random.nextInt(3, 6))
/** Draw once per product opening. Callers retain the returned list across recomposition. */
fun randomGallery(imageKey: String, random: Random = Random.Default): List<String> =
    (0..6).shuffled(random).take(random.nextInt(5, 8)).map { "${imageKey}_$it" }

@Serializable
data class OrderLine(val productId: String, val title: String, val variant: String, val quantity: Int, val unitPrice: Money)
@Serializable
data class CheckoutTotals(val subtotal: Money, val discount: Money, val tax: Money, val convenienceFee: Money,
    val total: Money, val offerCode: String?, val offerEligible: Boolean)
/** Demonstration pricing only: 18% tax on discounted merchandise, then a flat ₹29 fee. */
fun calculateTotals(lines: List<OrderLine>, offerCode: String?): CheckoutTotals {
    require(lines.all { it.quantity in 1..10 && it.unitPrice.minorUnits <= 100_000_000 })
    val subtotal = lines.sumOf { it.unitPrice.minorUnits * it.quantity }
    val offer = shoppingOffers.find { it.code == offerCode }
    val eligible = offer != null && subtotal >= offer.minimum
    val discount = if (eligible) minOf(subtotal, offer!!.cap,
        if (offer.percent > 0) (subtotal * offer.percent + 50) / 100 else offer.fixedDiscount) else 0L
    val taxable = subtotal - discount
    val tax = (taxable * 18 + 50) / 100
    val fee = if (lines.isEmpty() || (eligible && offer!!.waiveFee)) 0L else 2900L
    return CheckoutTotals(Money(subtotal), Money(discount), Money(tax), Money(fee),
        Money(taxable + tax + fee), offer?.code, eligible)
}
fun cartLines(cart: List<CartItem>, products: List<Product>): List<OrderLine> = cart.map { item ->
    val p = products.first { it.id == item.productId }
    require(item.variant in p.variants)
    OrderLine(p.id, p.title, item.variant, item.quantity, p.price)
}
@Serializable
data class PendingCheckout(val id: String, val mode: String, val lines: List<OrderLine>, val address: DemoAddress,
    val totals: CheckoutTotals, val createdAt: Long)
@Serializable
data class Receipt(val checkout: PendingCheckout, val paymentLabel: String)
data class CommerceSnapshot(val cart: List<CartItem> = emptyList(), val addresses: List<DemoAddress> = emptyList(),
    val selectedAddressId: String? = null, val offerCode: String? = null,
    val pending: PendingCheckout? = null, val receipts: List<Receipt> = emptyList())

interface CommerceRepository {
    fun observe(): Flow<CommerceSnapshot>
    suspend fun initialize()
    suspend fun addToCart(productId: String, variant: String)
    suspend fun setQuantity(productId: String, variant: String, quantity: Int)
    suspend fun saveAddress(address: DemoAddress)
    suspend fun deleteAddress(id: String)
    suspend fun selectAddress(id: String)
    suspend fun selectOffer(code: String?)
    suspend fun prepareCheckout(id: String, mode: String, now: Long): PendingCheckout
    suspend fun finalizeCheckout(id: String, verifiedTotal: Long, paymentLabel: String): Receipt
    suspend fun abandonCheckout(id: String)
}
sealed interface PaymentResult {
    data class Verified(val checkoutId: String, val amount: Long) : PaymentResult
    data class Pending(val message: String) : PaymentResult
    data class Failed(val message: String) : PaymentResult
    data object Cancelled : PaymentResult
}
interface SandboxPaymentGateway {
    val configured: Boolean
    suspend fun pay(checkout: PendingCheckout): PaymentResult
    suspend fun reconcile(checkout: PendingCheckout): PaymentResult
}
