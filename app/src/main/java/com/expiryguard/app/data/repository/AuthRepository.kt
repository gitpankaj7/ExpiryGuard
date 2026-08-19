package com.expiryguard.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody


class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val _currentUserFlow = MutableStateFlow(auth.currentUser)
    
    val loggedInUserIdFlow: Flow<String?> = _currentUserFlow.map { it?.uid }
    val currentUserFlow: Flow<FirebaseUser?> = _currentUserFlow

    init {
        auth.addAuthStateListener {
            _currentUserFlow.value = it.currentUser
        }
    }

    suspend fun register(
        name: String,
        email: String,
        passwordRaw: String
    ) {
        val result = auth.createUserWithEmailAndPassword(email, passwordRaw).await()
        val user = result.user ?: return
        
        _currentUserFlow.value = user

        val userProfile = hashMapOf(
            "uid" to user.uid,
            "name" to name,
            "email" to email,
            "trialStart" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "isSubscribed" to false
        )
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .set(userProfile)
            .await()
    }

    suspend fun login(email: String, passwordRaw: String) {
        val result = auth.signInWithEmailAndPassword(email, passwordRaw).await()
        _currentUserFlow.value = result.user
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun resetPassword(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteAccount(): Boolean {
        val user = auth.currentUser ?: return false
        val uid = user.uid
        return try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            
            // Delete all products for this user
            val productsRef = db.collection("users").document(uid).collection("products")
            val productsSnapshot = productsRef.get().await()
            for (doc in productsSnapshot.documents) {
                doc.reference.delete().await()
            }
            
            // Delete user account from auth
            user.delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Live Render.com URL
    private val BASE_URL = "https://expiryguard-backend-ik07.onrender.com"
    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val gson = com.google.gson.Gson()

    suspend fun createRazorpayOrder(): Map<String, Any>? {
        val user = auth.currentUser ?: return null
        return try {
            val tokenResult = user.getIdToken(true).await()
            val token = tokenResult.token ?: return null

            val request = okhttp3.Request.Builder()
                .url("$BASE_URL/create-order")
                .post(ByteArray(0).toRequestBody(null)) // Empty body
                .addHeader("Authorization", "Bearer $token")
                .build()

            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val responseBody = response.body?.string() ?: return@withContext null
                    val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                    gson.fromJson<Map<String, Any>>(responseBody, mapType)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun verifyRazorpayPayment(
        orderId: String,
        paymentId: String,
        signature: String
    ): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val tokenResult = user.getIdToken(true).await()
            val token = tokenResult.token ?: return false

            val data = mapOf(
                "razorpay_order_id" to orderId,
                "razorpay_payment_id" to paymentId,
                "razorpay_signature" to signature
            )
            
            val jsonBody = gson.toJson(data)
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = okhttp3.Request.Builder()
                .url("$BASE_URL/verify-payment")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $token")
                .build()

            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
