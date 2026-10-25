package com.parcel.presentation

import androidx.lifecycle.ViewModelStore
import com.parcel.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private class Store : AuthRepository {
        val value = MutableStateFlow(AuthSnapshot())
        var loadFails = false
        var requests = 0
        override fun observe() = flow { if (loadFails) error("Unavailable") else emitAll(value) }
        override suspend fun requestOtp(countryIso: String, number: String) { requests++; delay(10); error("Storage unavailable") }
        override suspend fun resendOtp() = Unit
        override suspend fun verifyOtp(code: String) = Unit
        override suspend fun register(firstName: String, lastName: String) = Unit
        override suspend fun changePhone() = Unit
        override suspend fun signOut() { value.value = AuthSnapshot() }
    }
    @Test fun failedRequestDoesNotAdvanceAndDoubleTapDoesNotDuplicate() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = Store(); val vm = AuthViewModel(repo); val owner = ViewModelStore().also { it.put("auth", vm) }
        try {
            runCurrent(); vm.request("IN", "9000000000"); vm.request("IN", "9000000000")
            advanceUntilIdle()
            assertEquals(1, repo.requests); assertNull(vm.state.value.snapshot.challenge)
            assertNotNull(vm.state.value.error); assertFalse(vm.state.value.busy)
        } finally { owner.clear(); Dispatchers.resetMain() }
    }
    @Test fun failedSessionReadCanRetryAndLogoutClearsTheObservedProfile() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = Store().apply { loadFails = true }; val vm = AuthViewModel(repo)
        val owner = ViewModelStore().also { it.put("auth", vm) }
        try {
            runCurrent(); assertTrue(vm.state.value.loadError)
            repo.loadFails = false; repo.value.value = AuthSnapshot(ShopperProfile("+919000000000", "Test", "Shopper"))
            vm.reload(); runCurrent()
            assertFalse(vm.state.value.loadError); assertNotNull(vm.state.value.snapshot.profile)
            vm.signOut(); runCurrent(); assertNull(vm.state.value.snapshot.profile)
        } finally { owner.clear(); Dispatchers.resetMain() }
    }
}
