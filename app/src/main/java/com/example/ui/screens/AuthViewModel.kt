package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DebugLogger
import com.example.data.LogCategory
import com.example.data.SessionManager
import com.example.data.ValidationErrorResponse
import com.example.data.api.BizoService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val bizoService: BizoService,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage

    fun clearMessages() {
        _error.value = null
        _infoMessage.value = null
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        DebugLogger.info(LogCategory.AUTH, "Tentative de connexion", "Email: $email")
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            try {
                val response = bizoService.login(mapOf("email" to email, "password" to password))
                sessionManager.saveAuthToken(response.token)
                sessionManager.saveUser(response.user)
                sessionManager.setHasSeenOnboarding(true)
                DebugLogger.success(LogCategory.AUTH, "Connexion réussie", "User: ${response.user.display_name}")
                onSuccess()
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.AUTH, "Échec de connexion", e.message)
                _error.value = parseApiMessage(e, "Identifiants incorrects ou erreur réseau")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit) {
        DebugLogger.info(LogCategory.AUTH, "Tentative d'inscription", "Name: $name, Email: $email")
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
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
                sessionManager.setHasSeenOnboarding(true)
                DebugLogger.success(LogCategory.AUTH, "Inscription réussie", "User: ${response.user.display_name}")
                onSuccess()
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.AUTH, "Échec d'inscription", e.message)
                _error.value = parseApiMessage(e, "Erreur lors de l'inscription")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun requestPasswordReset(email: String, onSuccess: () -> Unit = {}) {
        DebugLogger.info(LogCategory.AUTH, "Demande de réinitialisation", "Email: $email")
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            try {
                val response = bizoService.requestPasswordReset(mapOf("email" to email))
                _infoMessage.value = response.message
                onSuccess()
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.AUTH, "Échec demande reset", e.message)
                _error.value = parseApiMessage(e, "Impossible d'envoyer la demande de réinitialisation.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePassword(
        token: String,
        email: String,
        password: String,
        passwordConfirmation: String,
        onSuccess: () -> Unit = {}
    ) {
        DebugLogger.info(LogCategory.AUTH, "Tentative de nouveau mot de passe", "Email: $email")
        viewModelScope.launch {
            _isLoading.value = true
            clearMessages()
            try {
                val response = bizoService.updatePassword(
                    mapOf(
                        "token" to token,
                        "email" to email,
                        "password" to password,
                        "password_confirmation" to passwordConfirmation
                    )
                )
                _infoMessage.value = response.message
                onSuccess()
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.AUTH, "Échec mise à jour mot de passe", e.message)
                _error.value = parseApiMessage(e, "Impossible de réinitialiser le mot de passe.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseApiMessage(exception: Exception, fallback: String): String {
        if (exception is HttpException) {
            val body = exception.response()?.errorBody()?.string().orEmpty()
            if (body.isNotBlank()) {
                runCatching {
                    val validation = Json { ignoreUnknownKeys = true }
                        .decodeFromString<ValidationErrorResponse>(body)
                    validation.errors.values.firstOrNull()?.firstOrNull()
                        ?: validation.message
                }.getOrNull()?.let { return it }
            }
        }
        return fallback
    }
}
