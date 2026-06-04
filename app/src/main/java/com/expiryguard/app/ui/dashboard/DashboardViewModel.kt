package com.expiryguard.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expiryguard.app.ExpiryGuardApp
import com.expiryguard.app.data.local.ProductEntity
import com.expiryguard.app.data.repository.FirestoreProductRepository
import com.expiryguard.app.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class DashboardUiState(
    val totalProducts: Int = 0,
    val expiring7Days: Int = 0,
    val expiring30Days: Int = 0,
    val expiredProducts: Int = 0,
    val recentExpiringProducts: List<ProductEntity> = emptyList()
)

class DashboardViewModel(
    private val repository: FirestoreProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val now = DateUtils.todayStartMillis()
        val in7Days = now + TimeUnit.DAYS.toMillis(7)
        val in30Days = now + TimeUnit.DAYS.toMillis(30)

        viewModelScope.launch {
            combine(
                repository.getTotalCount(),
                repository.getExpiredCount(now),
                repository.getExpiringCount(now, in7Days),
                repository.getExpiringCount(now, in30Days),
                repository.getExpiringProducts(now, in30Days)
            ) { total, expired, exp7, exp30, expiringList ->
                DashboardUiState(
                    totalProducts = total,
                    expiredProducts = expired,
                    expiring7Days = exp7,
                    expiring30Days = exp30,
                    recentExpiringProducts = expiringList.take(5)
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.delete(product)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiryGuardApp
                DashboardViewModel(app.container.productRepository)
            }
        }
    }
}
