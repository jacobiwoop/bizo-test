package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.api.BizoService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val bizoService: BizoService,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        DebugLogger.info(LogCategory.AUTH, "Tentative de connexion", "Email: $email")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = bizoService.login(mapOf("email" to email, "password" to password))
                sessionManager.saveAuthToken(response.token)
                sessionManager.saveUser(response.user)
                DebugLogger.success(LogCategory.AUTH, "Connexion réussie", "User: ${response.user.display_name}")
                onSuccess()
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.AUTH, "Échec de connexion", e.message)
                _error.value = "Identifiants incorrects ou erreur réseau"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
        DebugLogger.info(LogCategory.AUTH, "Tentative d'inscription", "Name: $name, Email: $email")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = bizoService.register(
                    mapOf(
                        "display_name" to name,
                        "email" to email,
                        "password" to password,
                        "password_confirmation" to password
                    )
                )
                sessionManager.saveAuthToken(response.token)
                sessionManager.saveUser(response.user)
                DebugLogger.success(LogCategory.AUTH, "Inscription réussie", "User: ${response.user.display_name}")
                onSuccess()
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.AUTH, "Échec d'inscription", e.message)
                _error.value = "Erreur lors de l'inscription"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
