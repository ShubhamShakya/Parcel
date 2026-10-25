package com.parcel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parcel.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

fun newLocalId(): String = (1..24).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
data class CommerceState(val snapshot: CommerceSnapshot = CommerceSnapshot(), val loading: Boolean = true,
    val loadError: Boolean = false, val busy: Boolean = false, val error: String? = null,
    val notice: String? = null, val completedId: String? = null)

class CommerceViewModel(private val repo: CommerceRepository, private val gateway: SandboxPaymentGateway) : ViewModel() {
    private val mutableState = MutableStateFlow(CommerceState())
    val state = mutableState.asStateFlow()
    val offers = randomOfferDeck()
    val payUConfigured: Boolean get() = gateway.configured
    private var observation: kotlinx.coroutines.Job? = null
    init { reload() }
    fun reload() {
        observation?.cancel()
        observation = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, loadError = false) }
            try {
                repo.initialize()
                repo.observe().collect { snapshot -> mutableState.update { it.copy(snapshot = snapshot, loading = false, loadError = false) } }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableState.update { it.copy(loading = false, loadError = true) } }
        }
    }
    fun dismissError() { mutableState.update { it.copy(error = null) } }
    fun dismissNotice() { mutableState.update { it.copy(notice = null) } }
    fun consumeCompletion() { mutableState.update { it.copy(completedId = null) } }
    fun add(product: Product, variant: String, onAdded: () -> Unit = {}) = action {
        repo.addToCart(product.id, variant)
        onAdded()
        mutableState.update { it.copy(notice = "Added ${product.title} · $variant to your cart.") }
    }
    fun quantity(item: CartItem, quantity: Int) = action { repo.setQuantity(item.productId, item.variant, quantity) }
    fun offer(offer: Offer?) = action {
        repo.selectOffer(offer?.code)
        mutableState.update { it.copy(notice = offer?.let { selected -> "${selected.code} selected. ${selected.description}. It will apply automatically at checkout when eligible. One offer per order." } ?: "Offer removed.") }
    }
    fun saveAddress(address: DemoAddress) = action { repo.saveAddress(address) }
    fun deleteAddress(id: String) = action { repo.deleteAddress(id) }
    fun selectAddress(id: String) = action { repo.selectAddress(id) }
    fun placeDemo() = action {
        val pending = repo.prepareCheckout(newLocalId(), "demo", Clock.System.now().toEpochMilliseconds())
        val receipt = repo.finalizeCheckout(pending.id, pending.totals.total.minorUnits, "Local demo payment")
        mutableState.update { it.copy(completedId = receipt.checkout.id) }
    }
    fun payU() = action {
        check(gateway.configured) { "PayU sandbox is not configured. See the payment setup guide." }
        val pending = repo.prepareCheckout(newLocalId(), "payu", Clock.System.now().toEpochMilliseconds())
        handlePayment(pending, gateway.pay(pending))
    }
    fun reconcile() = action {
        val pending = state.value.snapshot.pending ?: error("No checkout to resume.")
        if (pending.mode == "demo") {
            val receipt = repo.finalizeCheckout(pending.id, pending.totals.total.minorUnits, "Local demo payment")
            mutableState.update { it.copy(completedId = receipt.checkout.id) }
        } else handlePayment(pending, gateway.reconcile(pending))
    }
    private suspend fun handlePayment(pending: PendingCheckout, result: PaymentResult) {
        when (result) {
            is PaymentResult.Verified -> {
                check(result.checkoutId == pending.id) { "Payment verification did not match this order." }
                val receipt = repo.finalizeCheckout(pending.id, result.amount, "PayU sandbox · verified")
                mutableState.update { it.copy(completedId = receipt.checkout.id) }
            }
            is PaymentResult.Pending -> mutableState.update { it.copy(error = result.message) }
            is PaymentResult.Failed -> { repo.abandonCheckout(pending.id); mutableState.update { it.copy(error = result.message) } }
            PaymentResult.Cancelled -> { repo.abandonCheckout(pending.id); mutableState.update { it.copy(notice = "Test payment cancelled. Your cart is unchanged.") } }
        }
    }
    private fun action(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { mutableState.update { it.copy(error = error.message ?: "Could not save this change. Please try again.") } }
            finally { mutableState.update { it.copy(busy = false) } }
        }
    }
}
