package com.aliaktas.urbanscore.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    // Common loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // One-time events for errors, navigation, etc.
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    // Error handler for coroutines
    protected val errorHandler = CoroutineExceptionHandler { _, throwable ->
        viewModelScope.launch {
            handleError(throwable)
        }
    }


    // Launch coroutines with error handling
    protected fun launchWithLoading(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(errorHandler) {
            try {
                setLoading(true)
                block()
            } finally {
                setLoading(false)
            }
        }
    }

    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is java.net.UnknownHostException,
            is java.net.ConnectException -> "No internet connection. Please check your network and try again."
            is com.google.firebase.FirebaseNetworkException -> "Network error. Please check your connection."
            is com.google.firebase.auth.FirebaseAuthException -> "Authentication error. Please try again."
            else -> "Something went wrong. Please try again later." // Exception mesajını gösterme
            // Logging için yine de hatayı loglayabilirsiniz:
            // Log.e("BaseViewModel", "Error details: ${throwable.message}", throwable)
        }
    }

    // Launch without showing loading state
    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(errorHandler) {
            block()
        }
    }

    // Set loading state
    protected fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    // Emit an event
    protected suspend fun emitEvent(event: UiEvent) {
        _events.emit(event)
    }

    // Handle errors
    protected open suspend fun handleError(throwable: Throwable) {
        val errorMessage = when (throwable) {
            is java.net.UnknownHostException,
            is java.net.ConnectException -> "No internet connection. Please check your network and try again."
            is com.google.firebase.FirebaseNetworkException -> "Network error. Please check your connection."
            is com.google.firebase.auth.FirebaseAuthException -> "Authentication error: ${simplifyAuthError(throwable)}"
            else -> "An unexpected error occurred: ${throwable.message ?: "Unknown error"}"
        }

        emitEvent(UiEvent.Error(errorMessage))
    }

    // Simplify Firebase auth errors for users
    private fun simplifyAuthError(error: com.google.firebase.auth.FirebaseAuthException): String {
        return when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email format"
            "ERROR_WRONG_PASSWORD" -> "Incorrect password"
            "ERROR_USER_NOT_FOUND" -> "User not found"
            "ERROR_USER_DISABLED" -> "This account has been disabled"
            "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later"
            "ERROR_OPERATION_NOT_ALLOWED" -> "This operation is not allowed"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use by another account"
            "ERROR_WEAK_PASSWORD" -> "Password is too weak"
            else -> "Authentication failed"
        }
    }

    // UI events sealed class for one-time events
    sealed class UiEvent {
        data class Error(val message: String) : UiEvent()
        data class Success(val message: String) : UiEvent()
        data class Loading(val message: String) : UiEvent()
        data class Navigate(val route: String) : UiEvent()
        data object ShowLogin : UiEvent()
        data object RefreshData : UiEvent()
    }
}