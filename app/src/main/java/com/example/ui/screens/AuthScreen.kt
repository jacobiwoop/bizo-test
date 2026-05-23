package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.Black
import com.example.ui.theme.GraySurface
import com.example.ui.theme.GrayText

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    var phone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.ui.theme.White)
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
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = "Entre ton numéro",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "On t'envoie un code SMS pour te connecter. Pas de mot de passe.",
            style = MaterialTheme.typography.bodyLarge,
            color = GrayText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(GraySurface, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🇧🇯", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Text("+229", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Black)
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier.width(1.dp).height(24.dp).background(GrayText.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.width(16.dp))
                BasicTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Black),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (phone.isEmpty()) {
                            Text("Numéro de téléphone", color = GrayText)
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(text = "Recevoir le code ->", onClick = onAuthenticated)
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            "🔒 Ton numéro reste privé et n'est jamais partagé.",
            color = GrayText,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
