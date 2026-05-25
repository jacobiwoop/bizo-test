package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bizo_prefs", Context.MODE_PRIVATE)

    fun saveAuthToken(token: String) {
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
        prefs.edit().apply {
            putString("user_id", user.id)
            putString("user_name", user.display_name)
            putString("user_email", user.email)
            putString("user_photo", user.photo_url)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
