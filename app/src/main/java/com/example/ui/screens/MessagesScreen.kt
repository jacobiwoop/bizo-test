package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.TransactionType
import com.example.ui.theme.Black
import com.example.ui.theme.GraySurface
import com.example.ui.theme.GrayText
import com.example.ui.theme.White

@Composable
fun MessagesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        // Simple Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Messages", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Black)
        }
        
        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(48.dp)
                .background(GraySurface, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Rechercher une conversation...", color = GrayText, style = MaterialTheme.typography.bodyLarge)
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn {
            items(5) { index ->
                ConversationItem(
                    name = listOf("Moussa K.", "Aïcha B.", "Désiré", "Fatoumata", "Kouassi")[index],
                    itemTitle = listOf("iPhone 13", "Mountain Bike", "Table en bois", "Robe wax", "Console")[index],
                    lastMessage = "Oui toujours dispo ! Tu es intéressé ?",
                    time = listOf("2 min", "1h", "3h", "Hier", "2j")[index],
                    unread = index == 0
                )
            }
        }
    }
}

@Composable
fun ConversationItem(name: String, itemTitle: String, lastMessage: String, time: String, unread: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(GraySurface)) {
            // Mock avatar
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Black)
                Spacer(modifier = Modifier.weight(1f))
                Text(time, style = MaterialTheme.typography.labelSmall, color = GrayText)
            }
            Text(itemTitle, style = MaterialTheme.typography.labelSmall, color = GrayText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (unread) Black else GrayText,
                fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (unread) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Black))
        }
    }
}
