package com.parcel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parcel.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CompanionState(val snapshot: CompanionSnapshot = CompanionSnapshot(), val loading: Boolean = true, val busy: Boolean = false, val loadError: Boolean = false, val error: String? = null, val message: String? = null)
class CompanionViewModel(private val repo: CompanionRepository) : ViewModel() {
    private val mutable = MutableStateFlow(CompanionState())
    val state = mutable.asStateFlow()
    private var observation: kotlinx.coroutines.Job? = null
    init { reload() }
    fun reload() {
        observation?.cancel()
        observation = viewModelScope.launch {
            try { repo.observe().collect { snapshot -> mutable.update { it.copy(snapshot = snapshot, loading = false, loadError = false) } } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(loading = false, loadError = true, error = "Could not load account details. Please retry.") } }
        }
    }
    fun dismiss() { mutable.update { it.copy(error = null, message = null) } }
    fun run(message: String? = "Saved on this device", action: suspend CompanionRepository.() -> Unit) {
        if (state.value.busy || state.value.loading || state.value.loadError) return
        mutable.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try { repo.action(); mutable.update { it.copy(message = message) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = e.message ?: "Unable to save. Try again.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
