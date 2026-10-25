package com.parcel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parcel.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DeliveryState(val snapshot: DeliverySnapshot = DeliverySnapshot(), val loading: Boolean = true,
    val busy: Boolean = false, val error: String? = null)
class DeliveryViewModel(private val repository: DeliveryRepository) : ViewModel() {
    private val mutable = MutableStateFlow(DeliveryState())
    val state = mutable.asStateFlow()
    private var observation: Job? = null
    init { reload() }
    fun reload() {
        observation?.cancel()
        observation = viewModelScope.launch {
            try {
                repository.refresh()
                repository.observe().collect { snapshot -> mutable.update { it.copy(snapshot = snapshot, loading = false) } }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(loading = false, error = "Could not load your parcels. Please retry.") } }
        }
    }
    suspend fun refresh() {
        try { repository.refresh() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(error = "Could not update tracking. Please retry.") } }
    }
    fun deliverNow(id: String) = action { repository.deliverNow(id) }
    fun unbox(id: String) = action { repository.unbox(id) }
    fun place(slot: Int, itemId: String) = action { repository.place(slot, itemId) }
    fun clear(slot: Int) = action { repository.clearSlot(slot) }
    fun dismissError() { mutable.update { it.copy(error = null) } }
    private fun action(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = e.message ?: "Could not save this change.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
