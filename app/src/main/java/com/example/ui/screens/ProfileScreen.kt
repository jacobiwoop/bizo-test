package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.Dependencies
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.Black
import com.example.ui.theme.White
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(onNavigateToMyListings: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = Dependencies.getSessionManager(context)
    val bizoService = Dependencies.getBizoService(context)
    val user by sessionManager.userData.collectAsState(initial = null)
    val email = user?.email ?: "Non renseigné"
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(20.dp)
    ) {
        Text("Profil", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Black)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text("Email : $email", style = MaterialTheme.typography.bodyLarge)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("UID : ${user?.id}", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(32.dp))
        
        PrimaryButton(text = "Voir mes annonces", onClick = onNavigateToMyListings)

        Spacer(modifier = Modifier.weight(1f))
        
        SecondaryButton(text = "Se déconnecter", onClick = { 
            coroutineScope.launch {
                try { bizoService.logout() } catch (e: Exception) {}
            }
        })
    }
}
