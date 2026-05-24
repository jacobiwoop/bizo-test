package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore by preferencesDataStore(name = "settings")

class SessionManager(private val context: Context) {
    private val TOKEN_KEY = stringPreferencesKey("auth_token")
    private val USER_KEY = stringPreferencesKey("user_data")

    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val userData: Flow<UserResource?> = context.dataStore.data.map { preferences ->
        preferences[USER_KEY]?.let { Json.decodeFromString<UserResource>(it) }
    }

    suspend fun saveSession(token: String, user: UserResource) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_KEY] = Json.encodeToString(user)
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_KEY)
        }
    }
}
