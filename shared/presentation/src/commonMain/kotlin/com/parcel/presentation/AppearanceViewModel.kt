package com.parcel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parcel.domain.AppearanceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppearanceState(val dark: Boolean = true, val loaded: Boolean = false, val busy: Boolean = false, val error: String? = null)
class AppearanceViewModel(private val repository: AppearanceRepository) : ViewModel() {
    private val mutable = MutableStateFlow(AppearanceState())
    val state = mutable.asStateFlow()
    init { viewModelScope.launch {
        try { repository.observeDarkMode().collect { dark -> mutable.update { it.copy(dark = dark, loaded = true) } } }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.update { it.copy(loaded = true, error = "Could not load appearance settings.") } }
    } }
    fun dismissError() { mutable.update { it.copy(error = null) } }
    fun setDark(dark: Boolean) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { repository.setDarkMode(dark) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(error = "Could not save appearance. Please try again.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
