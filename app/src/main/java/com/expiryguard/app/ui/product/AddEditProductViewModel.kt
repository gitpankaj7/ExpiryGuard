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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AddEditProductUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val category: String = "",
    val quantity: String = "",
    val purchasePrice: String = "",
    val expiryDate: Long? = null,
    val nameError: String? = null,
    val categoryError: String? = null,
    val quantityError: String? = null,
    val expiryDateError: String? = null,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
    val barcode: String = "",
    val isLookingUp: Boolean = false,
    val lookupMessage: String? = null,
    val isEditing: Boolean = false
)

class AddEditProductViewModel(
    private val repository: FirestoreProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditProductUiState())
    val uiState: StateFlow<AddEditProductUiState> = _uiState.asStateFlow()

    private var editingProductId: String = ""

    fun loadProduct(productId: String) {
        editingProductId = productId
        viewModelScope.launch {
            val product = repository.getProductById(productId)
            if (product != null) {
                _uiState.update {
                    it.copy(
                        name = product.name,
                        category = product.category,
                        quantity = product.quantity.toString(),
                        purchasePrice = if (product.purchasePrice > 0) product.purchasePrice.toString() else "",
                        expiryDate = product.expiryDate,
                        isEditing = true
                    )
                }
            }
        }
    }

    fun onBarcodeChange(barcode: String) {
        val previousBarcode = _uiState.value.barcode
        _uiState.update { it.copy(barcode = barcode) }
        
        if (barcode.isNotBlank() && barcode != previousBarcode && barcode.length >= 8) {
            fetchProductDetails(barcode)
        }
    }

    private fun fetchProductDetails(barcode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLookingUp = true, lookupMessage = "Looking up barcode...") }
            try {
                val result = com.expiryguard.app.util.BarcodeProductLookup.lookup(barcode)

                if (result != null) {
                    _uiState.update { 
                        val newName = if (it.name.isBlank()) result.name else it.name
                        val newCategory = if (it.category.isBlank() && result.category.isNotEmpty()) result.category else it.category
                        
                        it.copy(
                            isLookingUp = false, 
                            lookupMessage = "✅ Found: ${result.name}",
                            name = newName,
                            category = newCategory
                        ) 
                    }
                } else {
                    _uiState.update { it.copy(isLookingUp = false, lookupMessage = "⚠️ Product not found in database") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLookingUp = false, lookupMessage = "⚠️ Network error during lookup") }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun onCategoryChange(category: String) {
        _uiState.update { it.copy(category = category, categoryError = null) }
    }

    fun onQuantityChange(quantity: String) {
        _uiState.update { it.copy(quantity = quantity, quantityError = null) }
    }

    fun onPurchasePriceChange(price: String) {
        _uiState.update { it.copy(purchasePrice = price) }
    }

    fun onExpiryDateChange(date: Long) {
        _uiState.update { it.copy(expiryDate = date, expiryDateError = null) }
    }

    fun saveProduct() {
        val currentState = _uiState.value
        var hasError = false

        if (currentState.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Product name is required") }
            hasError = true
        }
        if (currentState.category.isBlank()) {
            _uiState.update { it.copy(categoryError = "Category is required") }
            hasError = true
        }
        val qty = currentState.quantity.toIntOrNull()
        if (qty == null || qty <= 0) {
            _uiState.update { it.copy(quantityError = "Enter a valid quantity") }
            hasError = true
        }
        if (currentState.expiryDate == null) {
            _uiState.update { it.copy(expiryDateError = "Expiry date is required") }
            hasError = true
        }

        if (hasError) return

        _uiState.update { it.copy(isLoading = true) }

        val price = currentState.purchasePrice.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            val product = ProductEntity(
                id = editingProductId,
                name = currentState.name.trim(),
                category = currentState.category.trim(),
                quantity = qty!!,
                purchasePrice = price,
                expiryDate = currentState.expiryDate!!,
                barcode = currentState.barcode.ifBlank { null }
            )

            try {
                if (editingProductId.isEmpty()) {
                    repository.insert(product)
                } else {
                    repository.update(product)
                }
                _uiState.update { it.copy(isSaved = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save product", isLoading = false) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpiryGuardApp
                AddEditProductViewModel(app.container.productRepository)
            }
        }
    }
}
