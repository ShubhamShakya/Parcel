package com.parcel.presentation

import androidx.lifecycle.ViewModelStore
import com.parcel.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    private val products = listOf(Product("one", "Headphones", "Wireless", Category.Tech,
        Money(10000), Money(20000), "headphones", "Sample", listOf("Black"), emptyList()))
    private class Store : SavedItemsRepository, PreferencesRepository {
        val ids = MutableStateFlow<Set<String>>(emptySet())
        val accepted = MutableStateFlow(false)
        var failWrites = false
        override fun observeSavedIds() = ids
        override fun observeDisclosureAccepted() = accepted
        override suspend fun setSaved(productId: String, saved: Boolean) {
            if (failWrites) error("Disk full")
            ids.value = if (saved) ids.value + productId else ids.value - productId
        }
        override suspend fun acceptDisclosure() { accepted.value = true }
    }
    @Test fun onboardingSearchAndSavingUseRepositoryState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = Store()
        val vm = CatalogViewModel(object : CatalogRepository { override fun observeProducts() = flowOf(products) }, store, store)
        val owner = ViewModelStore().also { it.put("catalog", vm) }
        try {
            runCurrent()
            assertFalse(vm.state.value.loading)
            assertEquals(false, vm.state.value.disclosureAccepted)
            vm.acceptDisclosure(); runCurrent()
            assertEquals(true, vm.state.value.disclosureAccepted)
            vm.search("missing"); assertTrue(vm.state.value.visibleProducts.isEmpty())
            vm.search("wireless"); assertEquals(products, vm.state.value.visibleProducts)
            vm.toggleSaved(products.first()); runCurrent()
            assertEquals(setOf("one"), vm.state.value.savedIds)
            vm.toggleSaved(products.first()); runCurrent()
            assertTrue(vm.state.value.savedIds.isEmpty())
        } finally { owner.clear(); Dispatchers.resetMain() }
    }
    @Test fun failedWriteDoesNotPretendItemWasSaved() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = Store().apply { failWrites = true }
        val vm = CatalogViewModel(object : CatalogRepository { override fun observeProducts() = flowOf(products) }, store, store)
        val owner = ViewModelStore().also { it.put("catalog", vm) }
        try {
            runCurrent(); vm.toggleSaved(products.first()); runCurrent()
            assertTrue(vm.state.value.savedIds.isEmpty())
            assertNotNull(vm.state.value.actionError)
            assertFalse(vm.state.value.busy)
        } finally { owner.clear(); Dispatchers.resetMain() }
    }
    @Test fun failedLoadCanBeRetried() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var fail = true
        val store = Store()
        val vm = CatalogViewModel(object : CatalogRepository {
            override fun observeProducts() = flow { if (fail) error("Unavailable") else emit(products) }
        }, store, store)
        val owner = ViewModelStore().also { it.put("catalog", vm) }
        try {
            runCurrent(); assertTrue(vm.state.value.loadError)
            fail = false; vm.reload(); runCurrent()
            assertFalse(vm.state.value.loadError); assertEquals(products, vm.state.value.products)
        } finally { owner.clear(); Dispatchers.resetMain() }
    }
    @Test fun switchingDepartmentClearsIncompatibleClothingFilters() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = Store()
        val vm = CatalogViewModel(object : CatalogRepository { override fun observeProducts() = flowOf(products) }, store, store)
        val owner = ViewModelStore().also { it.put("catalog", vm) }
        try {
            runCurrent()
            vm.applyClothingFilter(ClothingFilter(typeId = "hoodies", winterOnly = true, size = "M", fabric = "Cotton fleece"))
            vm.selectClothingType("coats")
            assertTrue(vm.state.value.clothingFilter.winterOnly)
            assertNull(vm.state.value.clothingFilter.size)
            vm.selectDepartment(ClothingDepartment.Women)
            assertEquals(ClothingFilter(department = ClothingDepartment.Women), vm.state.value.clothingFilter)
        } finally { owner.clear(); Dispatchers.resetMain() }
    }
}
