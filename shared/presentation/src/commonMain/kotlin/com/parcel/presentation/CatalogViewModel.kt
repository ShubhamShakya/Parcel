package com.parcel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parcel.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

data class CatalogState(
    val products: List<Product> = emptyList(),
    val savedIds: Set<String> = emptySet(),
    val disclosureAccepted: Boolean? = null,
    val query: String = "",
    val category: Category? = null,
    val clothingFilter: ClothingFilter = ClothingFilter(),
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val actionError: String? = null,
    val busy: Boolean = false,
) {
    val visibleProducts: List<Product> get() = filterProducts(products, query, category, clothingFilter)
    val savedProducts: List<Product> get() = products.filter { it.id in savedIds }
}

class CatalogViewModel(
    private val catalog: CatalogRepository,
    private val saved: SavedItemsRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CatalogState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null
    private val mutationMutex = Mutex()
    init { reload() }

    fun reload() {
        loadJob?.cancel()
        mutableState.update { it.copy(loading = true, loadError = false) }
        loadJob = viewModelScope.launch {
            combine(catalog.observeProducts(), saved.observeSavedIds(), preferences.observeDisclosureAccepted()) {
                products, ids, accepted -> Triple(products, ids, accepted)
            }.catch { error ->
                if (error is CancellationException) throw error
                mutableState.update { it.copy(loading = false, loadError = true) }
            }.collect { (products, ids, accepted) ->
                mutableState.update { it.copy(products = products, savedIds = ids,
                    disclosureAccepted = accepted, loading = false, loadError = false) }
            }
        }
    }
    fun search(query: String) { mutableState.update { it.copy(query = query) } }
    fun selectCategory(category: Category?) { mutableState.update { it.copy(category = category) } }
    fun selectDepartment(department: ClothingDepartment) { mutableState.update { it.copy(clothingFilter = ClothingFilter(department = department)) } }
    fun selectClothingType(id: String?) { mutableState.update { it.copy(clothingFilter = ClothingFilter(department = it.clothingFilter.department, typeId = id, winterOnly = it.clothingFilter.winterOnly)) } }
    fun selectWinter(enabled: Boolean) { mutableState.update { it.copy(clothingFilter = ClothingFilter(department = it.clothingFilter.department, winterOnly = enabled)) } }
    fun applyClothingFilter(filter: ClothingFilter) { mutableState.update { it.copy(clothingFilter = filter) } }
    fun resetClothingFilters() { mutableState.update { it.copy(query = "", clothingFilter = ClothingFilter(department = it.clothingFilter.department)) } }
    fun dismissError() { mutableState.update { it.copy(actionError = null) } }
    fun acceptDisclosure() = mutate("Could not save your preference. Please try again.") {
        preferences.acceptDisclosure()
    }
    fun toggleSaved(product: Product) = mutate("Could not update your wishlist. Please try again.") {
        // Idempotent writes and a single in-flight mutation avoid lost rapid-tap updates.
        saved.setSaved(product.id, product.id !in state.value.savedIds)
    }
    private fun mutate(message: String, block: suspend () -> Unit) {
        if (!mutationMutex.tryLock()) return
        mutableState.update { it.copy(busy = true, actionError = null) }
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableState.update { it.copy(actionError = message) } }
            finally { mutableState.update { it.copy(busy = false) }; mutationMutex.unlock() }
        }
    }
}
