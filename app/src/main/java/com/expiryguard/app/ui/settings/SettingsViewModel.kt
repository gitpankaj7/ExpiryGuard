package com.expiryguard.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expiryguard.app.ExpiryGuardApp
import com.expiryguard.app.data.preferences.UserPreferences
import com.expiryguard.app.data.repository.FirestoreProductRepository
import com.expiryguard.app.util.CsvExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val exportUri: Uri? = null,
    val showExportSuccess: Boolean = false
)

class SettingsViewModel(
    private val repository: FirestoreProductRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = userPreferences.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDarkMode(enabled)
        }
    }

    fun exportProducts(context: Context) {
        viewModelScope.launch {
            try {
                val products = repository.getAllProducts().first()
                val uri = CsvExporter.export(context, products)
                _uiState.update { it.copy(exportUri = uri, showExportSuccess = true) }
            } catch (_: Exception) {
                // Export failed silently
            }
        }
    }

    fun clearExportState() {
        _uiState.update { it.copy(exportUri = null, showExportSuccess = false) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiryGuardApp
                SettingsViewModel(
                    repository = app.container.productRepository,
                    userPreferences = app.container.userPreferences
                )
            }
        }
    }
}
