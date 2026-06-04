package com.expiryguard.app.di

import android.content.Context
import com.expiryguard.app.data.preferences.UserPreferences
import com.expiryguard.app.data.repository.AuthRepository
import com.expiryguard.app.data.repository.FirestoreProductRepository
import com.expiryguard.app.data.repository.UserRepository

class AppContainer(context: Context) {
    val authRepository = AuthRepository()
    val userRepository = UserRepository()
    val productRepository = FirestoreProductRepository(authRepository)
    val userPreferences = UserPreferences(context.applicationContext)
}
