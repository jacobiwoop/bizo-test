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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.example.ui.theme.GraySurface
import com.example.ui.theme.GrayText
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onNavigateToMyListings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = Dependencies.getSessionManager(context)
    val bizoService = Dependencies.getBizoService(context)
    val user by sessionManager.userData.collectAsState(initial = null)
    val email = user?.email ?: "Non renseigné"
    val displayName = user?.display_name ?: "Non renseigné"
    val bio = user?.bio ?: "Pas de bio"
    val rating = user?.rating ?: 0.0
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Mon Profil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Black)
        Spacer(modifier = Modifier.height(32.dp))
        
        // Avatar Placeholder
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(GraySurface, RoundedCornerShape(50.dp))
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Text(text = displayName.take(1).uppercase(), style = MaterialTheme.typography.displaySmall, color = GrayText)
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        ProfileItem(label = "Nom", value = displayName)
        ProfileItem(label = "Email", value = email)
        ProfileItem(label = "Bio", value = bio)
        ProfileItem(label = "Note", value = if (rating > 0) "$rating / 5" else "Aucun avis")
        
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(text = "Voir mes annonces", onClick = onNavigateToMyListings)
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryButton(text = "Mes favoris", onClick = onNavigateToFavorites)
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryButton(text = "Éditer le profil", onClick = onNavigateToEditProfile)

        Spacer(modifier = Modifier.weight(1f))
        
        SecondaryButton(text = "Se déconnecter", onClick = { 
            coroutineScope.launch {
                try { 
                    bizoService.logout()
                    onLogout()
                } catch (e: Exception) {
                    onLogout() // Navigate anyway if error
                }
            }
        })
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = GrayText)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Black)
    }
}
