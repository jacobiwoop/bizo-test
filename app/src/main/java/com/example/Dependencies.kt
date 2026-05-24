package com.example

import android.content.Context
import com.example.data.BizoService
import com.example.data.SessionManager
import com.example.data.apiClient

object Dependencies {
    private var sessionManager: SessionManager? = null
    private var bizoService: BizoService? = null

    fun getSessionManager(context: Context): SessionManager {
        return sessionManager ?: SessionManager(context.applicationContext).also { sessionManager = it }
    }

    fun getBizoService(context: Context): BizoService {
        return bizoService ?: BizoService(apiClient, getSessionManager(context)).also { bizoService = it }
    }
}
