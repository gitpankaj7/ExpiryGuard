package com.expiryguard.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

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
        
        val userProfile = hashMapOf(
            "uid" to user.uid,
            "name" to name,
            "email" to email,
            "trialStart" to System.currentTimeMillis(),
            "isSubscribed" to false
        )
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .set(userProfile)
            .await()
    }

    suspend fun login(email: String, passwordRaw: String) {
        auth.signInWithEmailAndPassword(email, passwordRaw).await()
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
}
