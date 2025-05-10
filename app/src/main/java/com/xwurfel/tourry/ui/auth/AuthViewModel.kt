package com.xwurfel.tourry.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.feature.auth.domain.model.User
import com.xwurfel.tourry.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val user: User) : AuthUiState()
    object Unauthenticated : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val isLoggedIn = authRepository.isLoggedIn()
            if (isLoggedIn) {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    _uiState.value = AuthUiState.Authenticated(user)
                } else {
                    _uiState.value = AuthUiState.Unauthenticated
                }
            } else {
                _uiState.value = AuthUiState.Unauthenticated
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                },
                onFailure = { exception ->
                    _uiState.value = AuthUiState.Error(exception.message ?: "Login failed")
                }
            )
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = authRepository.register(name, email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                },
                onFailure = { exception ->
                    _uiState.value = AuthUiState.Error(exception.message ?: "Registration failed")
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            authRepository.logout()
            _uiState.value = AuthUiState.Unauthenticated
        }
    }
}