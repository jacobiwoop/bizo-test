package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.example.Dependencies
import com.example.data.BizoService
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.Black
import com.example.ui.theme.GraySurface
import com.example.ui.theme.GrayText
import com.example.ui.theme.White
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(private val bizoService: BizoService) : ViewModel() {
    var email by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    fun resetPassword() {
        if (email.isEmpty()) {
            error = "Veuillez entrer votre email"
            return
        }
        isLoading = true
        error = null
        message = null
        viewModelScope.launch {
            try {
                val response = bizoService.forgotPassword(email)
                message = response.message ?: "Un email de réinitialisation a été envoyé."
                isLoading = false
            } catch (e: Exception) {
                error = "Une erreur est survenue. Vérifiez votre connexion."
                isLoading = false
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: ForgotPasswordViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ForgotPasswordViewModel(Dependencies.getBizoService(context)) as T
        }
    })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Black)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Réinitialisation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Black)
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Mot de passe oublié ?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Entre ton adresse email pour recevoir un lien de réinitialisation.",
            style = MaterialTheme.typography.bodyLarge,
            color = GrayText
        )

        Spacer(modifier = Modifier.height(32.dp))

        BasicTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Black),
            modifier = Modifier
                .fillMaxWidth()
                .background(GraySurface, RoundedCornerShape(12.dp))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                if (viewModel.email.isEmpty()) {
                    Text("Adresse email", color = GrayText)
                }
                innerTextField()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Black)
        } else {
            PrimaryButton(text = "Envoyer le lien ->", onClick = { viewModel.resetPassword() })
        }

        if (viewModel.message != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = viewModel.message!!, color = Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        if (viewModel.error != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = viewModel.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
