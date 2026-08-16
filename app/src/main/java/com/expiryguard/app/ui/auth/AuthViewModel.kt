package com.expiryguard.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expiryguard.app.ExpiryGuardApp
import com.expiryguard.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val resetEmailSent: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun register(name: String, email: String, passwordRaw: String) {
        if (name.isBlank() || email.isBlank() || passwordRaw.isBlank()) {
            showError("All fields are required.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                authRepository.register(name, email, passwordRaw)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                showError(e.localizedMessage ?: "Registration failed")
            }
        }
    }

    fun login(email: String, passwordRaw: String) {
        if (email.isBlank() || passwordRaw.isBlank()) {
            showError("Email and password are required.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                authRepository.login(email, passwordRaw)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                showError(e.localizedMessage ?: "Login failed")
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            showError("Email is required.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, resetEmailSent = false) }
            val success = authRepository.resetPassword(email)
            if (success) {
                _uiState.update { it.copy(isLoading = false, resetEmailSent = true) }
            } else {
                showError("Failed to send reset email. Check if the email is registered.")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val success = authRepository.deleteAccount()
            if (success) {
                authRepository.logout()
            } else {
                showError("Failed to delete account. Please try again.")
            }
        }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiryGuardApp
                AuthViewModel(app.container.authRepository)
            }
        }
    }
}
