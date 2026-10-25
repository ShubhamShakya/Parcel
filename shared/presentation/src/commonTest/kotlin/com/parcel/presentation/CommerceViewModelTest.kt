package com.parcel.presentation

import androidx.lifecycle.ViewModelStore
import com.parcel.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CommerceViewModelTest {
    private class Store : CommerceRepository {
        val line = OrderLine("sample", "Sample", "Sand", 1, Money(100000))
        val pending = PendingCheckout("frozen-checkout", "payu", listOf(line), demoAddresses.first(), calculateTotals(listOf(line), null), 1)
        val snapshot = MutableStateFlow(CommerceSnapshot(cart = listOf(CartItem("sample", "Sand", 1)), addresses = demoAddresses, selectedAddressId = demoAddresses.first().id))
        var prepared = 0; var finalized = 0
        override fun observe() = snapshot
        override suspend fun initialize() = Unit
        override suspend fun addToCart(productId: String, variant: String) = Unit
        override suspend fun setQuantity(productId: String, variant: String, quantity: Int) = Unit
        override suspend fun saveAddress(address: DemoAddress) = Unit
        override suspend fun deleteAddress(id: String) = Unit
        override suspend fun selectAddress(id: String) = Unit
        override suspend fun selectOffer(code: String?) = Unit
        override suspend fun prepareCheckout(id: String, mode: String, now: Long): PendingCheckout {
            prepared++; val checkout = pending.copy(mode = mode)
            snapshot.value = snapshot.value.copy(pending = checkout); return checkout
        }
        override suspend fun finalizeCheckout(id: String, verifiedTotal: Long, paymentLabel: String): Receipt {
            finalized++
            val receipt = Receipt(snapshot.value.pending!!, paymentLabel)
            snapshot.value = snapshot.value.copy(cart = emptyList(), pending = null, receipts = listOf(receipt)); return receipt
        }
        override suspend fun abandonCheckout(id: String) { snapshot.value = snapshot.value.copy(pending = null) }
    }
    private class Gateway(override val configured: Boolean = true) : SandboxPaymentGateway {
        var result: PaymentResult = PaymentResult.Pending("Awaiting verification")
        override suspend fun pay(checkout: PendingCheckout) = result
        override suspend fun reconcile(checkout: PendingCheckout) = result
    }
    @Test fun unavailableGatewayDoesNotCreateAPendingCheckout() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = Store(); val vm = CommerceViewModel(repo, Gateway(false))
        val owner = ViewModelStore().also { it.put("commerce", vm) }
        try {
            runCurrent(); vm.payU(); runCurrent()
            assertEquals(0, repo.prepared); assertEquals(0, repo.finalized)
            assertNotNull(vm.state.value.error); assertNull(repo.snapshot.value.pending)
        } finally { owner.clear(); Dispatchers.resetMain() }
    }
    @Test fun pendingPaymentPreservesCartAndOnlyVerifiedReconciliationCompletes() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = Store(); val gateway = Gateway(); val vm = CommerceViewModel(repo, gateway)
        val owner = ViewModelStore().also { it.put("commerce", vm) }
        try {
            runCurrent(); vm.payU(); runCurrent()
            assertEquals(0, repo.finalized); assertTrue(repo.snapshot.value.cart.isNotEmpty())
            assertNotNull(repo.snapshot.value.pending)
            gateway.result = PaymentResult.Verified(repo.pending.id, repo.pending.totals.total.minorUnits)
            vm.reconcile(); runCurrent()
            assertEquals(1, repo.finalized); assertTrue(repo.snapshot.value.cart.isEmpty())
            assertEquals(repo.pending.id, vm.state.value.completedId)
        } finally { owner.clear(); Dispatchers.resetMain() }
    }
    @Test fun verificationForAnotherCheckoutCannotFinalize() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = Store(); val gateway = Gateway().apply { result = PaymentResult.Verified("other", 1) }
        val vm = CommerceViewModel(repo, gateway); val owner = ViewModelStore().also { it.put("commerce", vm) }
        try {
            runCurrent(); vm.payU(); runCurrent()
            assertEquals(0, repo.finalized); assertNotNull(repo.snapshot.value.pending)
            assertNotNull(vm.state.value.error)
        } finally { owner.clear(); Dispatchers.resetMain() }
    }
    @Test fun duplicateSubmitDuringAnActionIsIgnored() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = Store(); val vm = CommerceViewModel(repo, Gateway())
        val owner = ViewModelStore().also { it.put("commerce", vm) }
        try {
            runCurrent(); vm.placeDemo(); vm.placeDemo(); runCurrent()
            assertEquals(1, repo.prepared); assertEquals(1, repo.finalized)
        } finally { owner.clear(); Dispatchers.resetMain() }
    }
}
