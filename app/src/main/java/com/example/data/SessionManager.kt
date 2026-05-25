package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bizo_prefs", Context.MODE_PRIVATE)

    fun saveAuthToken(token: String) {
        val maskedToken = if (token.length > 8) token.take(4) + "..." + token.takeLast(4) else "***"
        DebugLogger.info(LogCategory.AUTH, "Sauvegarde du token", "Token: $maskedToken")
        prefs.edit().putString("auth_token", token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString("user_id", userId).apply()
    }

    fun getUserId(): String? {
        return prefs.getString("user_id", null)
    }
    
    fun saveUser(user: UserResource) {
        DebugLogger.info(LogCategory.AUTH, "Mise à jour données utilisateur locale", "User: ${user.display_name}")
        prefs.edit().apply {
            putString("user_id", user.id)
            putString("user_name", user.display_name)
            putString("user_email", user.email)
            putString("user_photo", user.photo_url)
            apply()
        }
    }

    fun clearSession() {
        DebugLogger.warn(LogCategory.AUTH, "Session vidée localement")
        prefs.edit().clear().apply()
    }
}
