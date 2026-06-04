package com.expiryguard.app.data.local

import com.google.firebase.firestore.DocumentId

data class ProductEntity(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "",
    val quantity: Int = 0,
    val purchasePrice: Double = 0.0,
    val expiryDate: Long = 0L,
    val barcode: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
