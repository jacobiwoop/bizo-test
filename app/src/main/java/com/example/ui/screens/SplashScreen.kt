package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.SessionManager
import com.example.data.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, sessionManager: SessionManager) {
    LaunchedEffect(Unit) {
        DebugLogger.info(LogCategory.NAV, "Démarrage de l'application (Splash)")
        delay(1500) // Petit délai pour le branding
        val token = sessionManager.getAuthToken()
        if (token != null) {
            DebugLogger.success(LogCategory.AUTH, "Session active trouvée", "Token present")
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            DebugLogger.warn(LogCategory.AUTH, "Aucune session active", "Redirection vers Login")
            navController.navigate("auth") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "BIZO",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
