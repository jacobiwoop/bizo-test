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
import com.example.data.supabase
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.Black
import com.example.ui.theme.White
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(onNavigateToMyListings: () -> Unit) {
    val user = try { supabase.auth.currentSessionOrNull()?.user } catch (e: Exception) { null }
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
                try { supabase.auth.signOut() } catch (e: Exception) {}
            }
        })
    }
}
