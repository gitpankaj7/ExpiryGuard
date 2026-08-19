package com.expiryguard.app.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expiryguard.app.ExpiryGuardApp
import com.expiryguard.app.data.repository.FirestoreProductRepository
import com.expiryguard.app.data.preferences.UserPreferences
import com.expiryguard.app.util.DateUtils
import com.expiryguard.app.util.LossRecoveryEngine
import com.expiryguard.app.util.LossRecoveryReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class LossRecoveryUiState(
    val isLoading: Boolean = true,
    val report: LossRecoveryReport? = null,
    val errorMessage: String? = null
)

class LossRecoveryViewModel(
    private val repository: FirestoreProductRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LossRecoveryUiState())
    val uiState: StateFlow<LossRecoveryUiState> = _uiState.asStateFlow()

    init {
        loadAnalysis()
    }

    private fun loadAnalysis() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Fetch all products to run global analysis
                combine(
                    repository.getAllProducts(),
                    userPreferences.language
                ) { products, language ->
                    LossRecoveryEngine.analyze(products, language)
                }.collect { report ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            report = report
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error generating analysis: ${e.message}"
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiryGuardApp
                LossRecoveryViewModel(app.container.productRepository, app.container.userPreferences)
            }
        }
    }
}
