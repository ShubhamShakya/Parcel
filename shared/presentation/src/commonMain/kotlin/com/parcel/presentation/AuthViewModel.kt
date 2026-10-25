package com.parcel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parcel.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthState(val snapshot: AuthSnapshot = AuthSnapshot(), val loading: Boolean = true,
    val loadError: Boolean = false, val busy: Boolean = false, val error: String? = null)
class AuthViewModel(private val repo: AuthRepository) : ViewModel() {
    private val mutable = MutableStateFlow(AuthState())
    val state = mutable.asStateFlow()
    private var observation: Job? = null
    init { reload() }
    fun reload() {
        observation?.cancel()
        mutable.update { it.copy(loading = true, loadError = false) }
        observation = viewModelScope.launch {
            repo.observe().catch { e ->
                if (e is CancellationException) throw e
                mutable.update { it.copy(loading = false, loadError = true) }
            }.collect { snapshot -> mutable.update { it.copy(snapshot = snapshot, loading = false, loadError = false) } }
        }
    }
    fun request(country: String, number: String) = action { repo.requestOtp(country, number) }
    fun resend() = action { repo.resendOtp() }
    fun verify(code: String) = action { repo.verifyOtp(code) }
    fun register(first: String, last: String) = action { repo.register(first, last) }
    fun changePhone() = action { repo.changePhone() }
    fun signOut() = action { repo.signOut() }
    private fun action(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(error = e.message ?: "Please try again.") } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
}
