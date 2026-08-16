package com.expiryguard.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "expiryguard_settings")

class UserPreferences(private val context: Context) {

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_MODE] ?: false
    }

    val language: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LANGUAGE] ?: "en"
    }

    val loggedInUserId: Flow<Long?> = context.dataStore.data.map { preferences ->
        val id = preferences[PreferencesKeys.LOGGED_IN_USER_ID]
        if (id == -1L) null else id
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = lang
        }
    }

    suspend fun setLoggedInUserId(id: Long?) {
        context.dataStore.edit { preferences ->
            if (id == null) {
                preferences.remove(PreferencesKeys.LOGGED_IN_USER_ID)
            } else {
                preferences[PreferencesKeys.LOGGED_IN_USER_ID] = id
            }
        }
    }

    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LOGGED_IN_USER_ID = longPreferencesKey("logged_in_user_id")
        val LANGUAGE = stringPreferencesKey("language")
    }
}
