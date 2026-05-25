package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SessionManager
import com.example.data.api.BizoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val bizoService: BizoService,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = bizoService.login(mapOf("email" to email, "password" to password))
                sessionManager.saveAuthToken(response.token)
                sessionManager.saveUser(response.user)
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Identifiants incorrects ou erreur réseau"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
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
                onSuccess()
            } catch (e: Exception) {
                _error.value = "Erreur lors de l'inscription"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
