package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BizoService
import com.example.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthState {
    INPUT_CREDENTIALS,
    LOADING,
    SUCCESS,
    ERROR
}

class AuthViewModel(
    private val bizoService: BizoService,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _authState = MutableStateFlow(AuthState.INPUT_CREDENTIALS)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun signIn(email: String, parseword: String) {
        if (email.isEmpty() || parseword.isEmpty()) {
            _errorMessage.value = "Veuillez remplir tous les champs"
            return
        }
        _authState.value = AuthState.LOADING
        viewModelScope.launch {
            try {
                val response = bizoService.login(email, parseword)
                sessionManager.saveSession(response.token, response.user)
                _authState.value = AuthState.SUCCESS
            } catch (e: Exception) {
                _authState.value = AuthState.ERROR
                _errorMessage.value = "Email ou mot de passe incorrect"
            }
        }
    }

    fun signUp(email: String, parseword: String, displayName: String) {
        if (email.isEmpty() || parseword.isEmpty() || displayName.isEmpty()) {
            _errorMessage.value = "Veuillez remplir tous les champs"
            return
        }
        _authState.value = AuthState.LOADING
        viewModelScope.launch {
            try {
                val response = bizoService.register(email, parseword, displayName, null)
                sessionManager.saveSession(response.token, response.user)
                _authState.value = AuthState.SUCCESS
            } catch (e: Exception) {
                _authState.value = AuthState.ERROR
                _errorMessage.value = e.message ?: "Erreur lors de l'inscription"
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.INPUT_CREDENTIALS
        _errorMessage.value = null
    }
}

