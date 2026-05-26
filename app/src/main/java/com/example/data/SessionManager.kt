package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bizo_prefs", Context.MODE_PRIVATE)
    private val _authToken = MutableStateFlow(prefs.getString("auth_token", null))
    val authToken: StateFlow<String?> = _authToken.asStateFlow()
    private val _userId = MutableStateFlow(prefs.getString("user_id", null))
    val userId: StateFlow<String?> = _userId.asStateFlow()
    private val _hasSeenOnboarding = MutableStateFlow(prefs.getBoolean("has_seen_onboarding_local", false))
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

    fun saveAuthToken(token: String) {
        val maskedToken = if (token.length > 8) token.take(4) + "..." + token.takeLast(4) else "***"
        DebugLogger.info(LogCategory.AUTH, "Sauvegarde du token", "Token: $maskedToken")
        prefs.edit().putString("auth_token", token).apply()
        _authToken.value = token
    }

    fun getAuthToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString("user_id", userId).apply()
        _userId.value = userId
    }

    fun getUserId(): String? {
        return prefs.getString("user_id", null)
    }

    fun setHasSeenOnboarding(seen: Boolean = true) {
        prefs.edit().putBoolean("has_seen_onboarding_local", seen).apply()
        _hasSeenOnboarding.value = seen
    }

    fun hasSeenOnboarding(): Boolean {
        return prefs.getBoolean("has_seen_onboarding_local", false)
    }
    
    fun saveUser(user: UserResource) {
        DebugLogger.info(LogCategory.AUTH, "Mise à jour données utilisateur locale", "User: ${user.display_name}")
        val mergedOnboarding = hasSeenOnboarding() || (user.has_seen_onboarding == true)
        prefs.edit().apply {
            putString("user_id", user.id)
            putString("user_name", user.display_name)
            putString("user_email", user.email)
            putString("user_photo", user.photo_url)
            putBoolean("has_seen_onboarding_local", mergedOnboarding)
            apply()
        }
        _userId.value = user.id
        _hasSeenOnboarding.value = mergedOnboarding
    }

    fun clearSession() {
        DebugLogger.warn(LogCategory.AUTH, "Session vidée localement")
        prefs.edit().clear().apply()
        _authToken.value = null
        _userId.value = null
    }
}
