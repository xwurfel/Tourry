package com.xwurfel.tourry.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xwurfel.tourry.domain.auth.service.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isInitializing: Boolean = true,
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun checkAuthState() {
        viewModelScope.launch {
            val currentUser = authService.getCurrentUser()
            _uiState.update {
                it.copy(
                    isInitializing = false,
                    isAuthenticated = currentUser != null
                )
            }
        }
    }

    fun setAuthenticated(isAuthenticated: Boolean) {
        _uiState.update { it.copy(isAuthenticated = isAuthenticated) }
    }
}