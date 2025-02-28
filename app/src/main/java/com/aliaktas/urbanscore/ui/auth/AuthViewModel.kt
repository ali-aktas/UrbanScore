package com.aliaktas.urbanscore.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliaktas.urbanscore.data.model.UserModel
import com.aliaktas.urbanscore.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Initial)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        // Kullanıcının oturum durumunu kontrol et
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
            val result = userRepository.signInWithEmail(email, password)
            result.fold(
                onSuccess = { user ->
                    _state.value = AuthState.Authenticated(user)
                },
                onFailure = { exception ->
                    _state.value = AuthState.Error(exception.message ?: "Authentication failed")
                }
            )
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
                        _state.value = AuthState.Error(exception.message ?: "Sign up failed")
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Unexpected error during sign up", e)
                _state.value = AuthState.Error(e.message ?: "An unexpected error occurred")
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
                    _state.value = AuthState.Error(exception.message ?: "Google sign in failed")
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
            // State update will happen via the auth state listener in init
        }
    }

    // Hataları temizle (örn. error state'den çıkmak için)
    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Unauthenticated
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