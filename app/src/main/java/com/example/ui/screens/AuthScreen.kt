package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.Dependencies
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.Black
import com.example.ui.theme.GraySurface
import com.example.ui.theme.GrayText
import com.example.ui.theme.White

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(
                Dependencies.getBizoService(context),
                Dependencies.getSessionManager(context)
            ) as T
        }
    })

    val authState by viewModel.authState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }

    LaunchedEffect(authState) {
        if (authState == AuthState.SUCCESS) {
            onAuthenticated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Bizo",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Black
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (authState == AuthState.INPUT_CREDENTIALS || authState == AuthState.ERROR) {
            Text(
                text = if (isLoginMode) "Connexion" else "Inscription",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isLoginMode) "Connecte-toi avec ton email" else "Crée un compte avec ton email",
                style = MaterialTheme.typography.bodyLarge,
                color = GrayText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isLoginMode) {
                // Display Name Input
                BasicTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GraySurface, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    decorationBox = { innerTextField ->
                        if (displayName.isEmpty()) {
                            Text("Nom complet", color = GrayText)
                        }
                        innerTextField()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Email Input
            BasicTextField(
                value = email,
                onValueChange = { email = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraySurface, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                decorationBox = { innerTextField ->
                    if (email.isEmpty()) {
                        Text("Adresse email", color = GrayText)
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            BasicTextField(
                value = password,
                onValueChange = { password = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraySurface, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                decorationBox = { innerTextField ->
                    if (password.isEmpty()) {
                        Text("Mot de passe", color = GrayText)
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoginMode) {
                TextButton(
                    onClick = { onForgotPassword() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Mot de passe oublié ?", color = Black, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(text = if (isLoginMode) "Se connecter ->" else "S'inscrire ->", onClick = {
                if (isLoginMode) {
                    viewModel.signIn(email, password)
                } else {
                    viewModel.signUp(email, password, displayName)
                }
            })

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { isLoginMode = !isLoginMode }) {
                Text(
                    text = if (isLoginMode) "Pas de compte ? S'inscrire" else "Déjà un compte ? Se connecter",
                    color = Black,
                    fontWeight = FontWeight.Bold
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            }
        } else if (authState == AuthState.LOADING) {
            CircularProgressIndicator(color = Black)
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
