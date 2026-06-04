package com.expiryguard.app.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.expiryguard.app.ExpiryGuardApp
import com.expiryguard.app.data.local.ProductEntity
import com.expiryguard.app.data.repository.FirestoreProductRepository
import com.expiryguard.app.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProductFilter { ALL, EXPIRED, EXPIRING_SOON, SAFE }

data class ProductListUiState(
    val products: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: ProductFilter = ProductFilter.ALL,
    val isLoading: Boolean = true
)

class ProductListViewModel(
    private val repository: FirestoreProductRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val activeFilter = MutableStateFlow(ProductFilter.ALL)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ProductListUiState> = combine(
        searchQuery,
        activeFilter
    ) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        val productsFlow = if (query.isBlank()) {
            repository.getAllProducts()
        } else {
            repository.searchProducts(query)
        }
        productsFlow.map { products ->
            val filtered = when (filter) {
                ProductFilter.ALL -> products
                ProductFilter.EXPIRED -> products.filter {
                    DateUtils.getExpiryStatus(it.expiryDate) == DateUtils.ExpiryStatus.EXPIRED
                }
                ProductFilter.EXPIRING_SOON -> products.filter {
                    val status = DateUtils.getExpiryStatus(it.expiryDate)
                    status == DateUtils.ExpiryStatus.CRITICAL || status == DateUtils.ExpiryStatus.WARNING
                }
                ProductFilter.SAFE -> products.filter {
                    DateUtils.getExpiryStatus(it.expiryDate) == DateUtils.ExpiryStatus.SAFE
                }
            }
            ProductListUiState(
                products = filtered,
                searchQuery = query,
                activeFilter = filter,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProductListUiState()
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onFilterChange(filter: ProductFilter) {
        activeFilter.value = filter
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
                ProductListViewModel(app.container.productRepository)
            }
        }
    }
}
