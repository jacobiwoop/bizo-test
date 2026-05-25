package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SessionManager
import com.example.data.api.BizoService
import com.example.ui.components.PrimaryButton

@Composable
fun AuthScreen(navController: NavController, bizoService: BizoService, sessionManager: SessionManager) {
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(bizoService, sessionManager))
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val scrollState = rememberScrollState()

    var isLogin by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isLogin) "Connexion" else "Inscription",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        if (!isLogin) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom complet") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Check else Icons.Filled.Lock
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(image, contentDescription = null)
                }
            }
        )

        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryButton(
            text = if (isLogin) "Se connecter" else "S'inscrire",
            onClick = {
                if (!isLogin && name.isBlank()) {
                    return@PrimaryButton
                }
                if (email.isBlank() || password.isBlank()) {
                    return@PrimaryButton
                }
                if (isLogin) {
                    viewModel.login(email, password) {
                        navController.navigate("home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                } else {
                    viewModel.register(name, email, password) {
                        navController.navigate("home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                }
            },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && (isLogin || name.isNotBlank())
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { isLogin = !isLogin }) {
            Text(if (isLogin) "Pas encore de compte ? S'inscrire" else "Déjà un compte ? Se connecter")
        }
        
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
