package com.shubhamshakya.parcel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shubhamshakya.parcel.domain.AppearanceMode
import com.shubhamshakya.parcel.domain.AppearanceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppearanceState(val mode: AppearanceMode = AppearanceMode.System, val loaded: Boolean = false, val busy: Boolean = false, val error: String? = null)
class AppearanceViewModel(private val repository: AppearanceRepository) : ViewModel() {
    private val mutable = MutableStateFlow(AppearanceState())
    val state = mutable.asStateFlow()
    init { viewModelScope.launch {
        try { repository.observeAppearance().collect { mode -> mutable.update { it.copy(mode = mode, loaded = true) } } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(loaded = true, error = "Could not load appearance settings.") } }
    } }
    fun dismissError() { mutable.update { it.copy(error = null) } }
    fun setMode(mode: AppearanceMode) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { repository.setAppearance(mode) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(error = "Could not save appearance. Please try again.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
