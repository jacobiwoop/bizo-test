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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.Black
import com.example.ui.theme.White

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(20.dp)
    ) {
        Text("Profil", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Black)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text("Paramètres mockés pour démonstration.", style = MaterialTheme.typography.bodyLarge)
        
        Spacer(modifier = Modifier.weight(1f))
        
        SecondaryButton(text = "Se déconnecter", onClick = { })
    }
}
