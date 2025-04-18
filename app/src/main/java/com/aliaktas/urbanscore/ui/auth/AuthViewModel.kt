package com.aliaktas.urbanscore.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.UserModel
import com.aliaktas.urbanscore.data.repository.UserRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.ConnectException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Initial)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        // Check user authentication status
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    _state.value = AuthState.Authenticated(user)
                } else {
                    if (_state.value !is AuthState.Initial) {
                        _state.value = AuthState.Unauthenticated
                    }
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = userRepository.signInWithEmail(email, password)
                result.fold(
                    onSuccess = { user ->
                        _state.value = AuthState.Authenticated(user)
                    },
                    onFailure = { exception ->
                        _state.value = AuthState.Error(formatErrorMessage(exception))
                    }
                )
            } catch (e: Exception) {
                _state.value = AuthState.Error(formatErrorMessage(e))
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, displayName: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = userRepository.signUpWithEmail(email, password, displayName)
                result.fold(
                    onSuccess = { user ->
                        _state.value = AuthState.Authenticated(user)
                        Log.d("AuthViewModel", "Sign up successful: ${user.id}")
                    },
                    onFailure = { exception ->
                        Log.e("AuthViewModel", "Sign up failed", exception)
                        _state.value = AuthState.Error(formatErrorMessage(exception))
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Unexpected error during sign up", e)
                _state.value = AuthState.Error(formatErrorMessage(e))
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            val result = userRepository.signInWithGoogle(idToken)
            result.fold(
                onSuccess = { user ->
                    _state.value = AuthState.Authenticated(user)
                },
                onFailure = { exception ->
                    _state.value = AuthState.Error(formatErrorMessage(exception))
                }
            )
        }
    }


    // AuthViewModel.kt - saveUserCountry metodunu güncelle
    fun saveUserCountry(countryId: String) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Saving user country: $countryId")
                val result = userRepository.saveUserCountry(countryId)
                result.fold(
                    onSuccess = {
                        Log.d("AuthViewModel", "✓ Country saved successfully: $countryId")
                        // Kayıt başarılı mesajı ekleyebiliriz
                    },
                    onFailure = { e ->
                        Log.e("AuthViewModel", "✗ Failed to save country: ${e.message}")
                        _state.value = AuthState.Error("Failed to save country: ${e.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error saving user country", e)
                _state.value = AuthState.Error("Failed to save country: ${e.message}")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
            // State update will happen via the auth state listener in init
        }
    }

    // Clear errors (to exit error state)
    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Unauthenticated
        }
    }

    // Format error messages to be user-friendly
    private fun formatErrorMessage(exception: Throwable): String {
        return when (exception) {
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Invalid email format"
                    "ERROR_WRONG_PASSWORD" -> "Incorrect password"
                    "ERROR_USER_NOT_FOUND" -> "No account found with this email"
                    "ERROR_USER_DISABLED" -> "This account has been disabled"
                    "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts, please try again later"
                    "ERROR_OPERATION_NOT_ALLOWED" -> "This operation is currently unavailable"
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already in use"
                    "ERROR_WEAK_PASSWORD" -> "Password is too weak, use at least 6 characters"
                    else -> "Authentication failed"
                }
            }
            is FirebaseNetworkException -> "Please check your internet connection"
            is UnknownHostException, is ConnectException -> "Please check your internet connection"
            else -> "An unexpected error occurred. Please try again."
        }
    }
}

sealed class AuthState {
    data object Initial : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val user: UserModel) : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}