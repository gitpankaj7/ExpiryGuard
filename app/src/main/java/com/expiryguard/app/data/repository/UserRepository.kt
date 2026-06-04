package com.expiryguard.app.data.repository

import com.expiryguard.app.data.local.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

class UserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val userProfileFlow: Flow<UserProfile?> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            listenerRegistration?.remove()
            val user = firebaseAuth.currentUser
            if (user != null) {
                listenerRegistration = firestore.collection("users").document(user.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(null)
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val profile = snapshot.toObject(UserProfile::class.java)
                            trySend(profile)
                        } else {
                            val userProfile = hashMapOf(
                                "uid" to user.uid,
                                "name" to (user.displayName ?: ""),
                                "email" to (user.email ?: ""),
                                "trialStart" to System.currentTimeMillis(),
                                "isSubscribed" to false
                            )
                            firestore.collection("users").document(user.uid).set(userProfile)
                        }
                    }
            } else {
                trySend(null)
            }
        }

        auth.addAuthStateListener(authStateListener)

        awaitClose {
            auth.removeAuthStateListener(authStateListener)
            listenerRegistration?.remove()
        }
    }

    suspend fun simulateSubscription() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).update("isSubscribed", true)
    }
}
