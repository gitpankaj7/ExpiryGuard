package com.expiryguard.app.data.repository

import com.expiryguard.app.data.local.ProductEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreProductRepository(
    private val authRepository: AuthRepository
) {
    private val firestore = FirebaseFirestore.getInstance()

    private fun getProductsCollection() = authRepository.loggedInUserIdFlow.map { uid ->
        if (uid != null) firestore.collection("users").document(uid).collection("products") else null
    }

    private fun queryAsFlow(queryModifier: (Query) -> Query): Flow<List<ProductEntity>> {
        return getProductsCollection().flatMapLatest { collection ->
            if (collection == null) return@flatMapLatest flowOf(emptyList())
            
            callbackFlow {
                val subscription = queryModifier(collection).addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val products = snapshot.toObjects(ProductEntity::class.java)
                        trySend(products)
                    }
                }
                awaitClose { subscription.remove() }
            }
        }
    }

    fun getAllProducts(): Flow<List<ProductEntity>> = queryAsFlow { query ->
        query.orderBy("expiryDate", Query.Direction.ASCENDING)
    }

    fun searchProducts(searchQuery: String): Flow<List<ProductEntity>> {
        return getAllProducts().map { list ->
            if (searchQuery.isBlank()) list
            else list.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.category.contains(searchQuery, ignoreCase = true) 
            }
        }
    }

    fun getExpiringProducts(now: Long, threshold: Long): Flow<List<ProductEntity>> = queryAsFlow { query ->
        query.whereGreaterThanOrEqualTo("expiryDate", now)
             .whereLessThanOrEqualTo("expiryDate", threshold)
             .orderBy("expiryDate", Query.Direction.ASCENDING)
    }

    fun getTotalCount(): Flow<Int> = getAllProducts().map { it.size }

    fun getExpiredCount(now: Long): Flow<Int> = getAllProducts().map { list ->
        list.count { it.expiryDate < now }
    }

    fun getExpiringCount(now: Long, threshold: Long): Flow<Int> = getExpiringProducts(now, threshold).map { it.size }

    suspend fun getProductById(id: String): ProductEntity? {
        // We shouldn't use Flow here as it's a one-off suspend function.
        // But we need the uid. We can just use FirebaseAuth directly for one-offs.
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return try {
            val doc = firestore.collection("users").document(uid).collection("products").document(id).get().await()
            doc.toObject(ProductEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return try {
            val querySnapshot = firestore.collection("users").document(uid).collection("products")
                .whereEqualTo("barcode", barcode)
                .limit(1)
                .get()
                .await()
            if (!querySnapshot.isEmpty) {
                querySnapshot.documents[0].toObject(ProductEntity::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun insert(product: ProductEntity) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val collection = firestore.collection("users").document(uid).collection("products")
        // Create a new document with an auto-generated ID if the provided product id is empty
        val docRef = if (product.id.isEmpty()) collection.document() else collection.document(product.id)
        
        val productWithId = product.copy(id = docRef.id)
        docRef.set(productWithId)
    }

    suspend fun update(product: ProductEntity) {
        if (product.id.isEmpty()) return
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("products").document(product.id).set(product)
    }

    suspend fun delete(product: ProductEntity) {
        if (product.id.isEmpty()) return
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("products").document(product.id).delete()
    }
}
