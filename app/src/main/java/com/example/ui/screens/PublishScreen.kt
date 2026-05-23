package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.PrimaryButton
import com.example.ui.components.TransactionType
import com.example.ui.theme.Black
import com.example.ui.theme.GrayBorder
import com.example.ui.theme.GraySurface
import com.example.ui.theme.GrayText
import com.example.ui.theme.White

@Composable
fun PublishScreen(onBack: () -> Unit) {
    var selectedType by remember { mutableStateOf<TransactionType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Black)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publier une annonce", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Black)
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Text("Comment tu veux vendre ?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Choisis le mode de transaction qui te convient le mieux.", style = MaterialTheme.typography.bodyLarge, color = GrayText)
        
        Spacer(modifier = Modifier.height(24.dp))

        TransactionCardOption(
            title = "VENTE",
            desc = "Vends ton article au prix fort et reçois le paiement.",
            icon = "💰",
            selected = selectedType == TransactionType.VENTE,
            onClick = { selectedType = TransactionType.VENTE }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TransactionCardOption(
            title = "TROC",
            desc = "Échange ton article contre un autre objet de valeur similaire.",
            icon = "🔄",
            selected = selectedType == TransactionType.TROC,
            onClick = { selectedType = TransactionType.TROC }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TransactionCardOption(
            title = "TROC+CASH",
            desc = "Échange avec un ajout d'argent si l'objet proposé a moins de valeur.",
            icon = "🤝",
            selected = selectedType == TransactionType.TROC_CASH,
            onClick = { selectedType = TransactionType.TROC_CASH }
        )

        Spacer(modifier = Modifier.weight(1f))
        
        PrimaryButton(text = "Continuer ->", onClick = { /* TODO */ })
    }
}

@Composable
fun TransactionCardOption(
    title: String,
    desc: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Black else GrayBorder
    val bgColor = if (selected) GraySurface else White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Black)
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = GrayText)
        }
    }
}
