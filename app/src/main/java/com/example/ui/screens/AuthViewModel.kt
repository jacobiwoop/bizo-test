package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
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

class AuthViewModel : ViewModel() {
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
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = parseword
                }
                _authState.value = AuthState.SUCCESS
            } catch (e: Exception) {
                _authState.value = AuthState.ERROR
                _errorMessage.value = e.message
            }
        }
    }

    fun signUp(email: String, parseword: String) {
        if (email.isEmpty() || parseword.isEmpty()) {
            _errorMessage.value = "Veuillez remplir tous les champs"
            return
        }
        _authState.value = AuthState.LOADING
        viewModelScope.launch {
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = parseword
                }
                _authState.value = AuthState.SUCCESS
            } catch (e: Exception) {
                _authState.value = AuthState.ERROR
                _errorMessage.value = e.message
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.INPUT_CREDENTIALS
        _errorMessage.value = null
    }
}

