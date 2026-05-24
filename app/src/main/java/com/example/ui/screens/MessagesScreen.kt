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

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.Dependencies
import com.example.data.ConversationResource

@Composable
fun MessagesScreen() {
    val context = LocalContext.current
    val viewModel: MessagesViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MessagesViewModel(Dependencies.getBizoService(context)) as T
        }
    })
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Messages", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Black)
        }
        
        if (isLoading && conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Black)
            }
        } else if (conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune conversation", color = GrayText)
            }
        } else {
            LazyColumn {
                items(conversations.size) { index ->
                    val conversation = conversations[index]
                    ConversationItem(
                        name = conversation.other_user?.display_name ?: "Utilisateur",
                        itemTitle = conversation.listing_title,
                        lastMessage = conversation.last_message ?: "Pas de message",
                        time = "Récemment", // Format date properly later
                        unread = conversation.unread_count > 0,
                        avatarUrl = conversation.other_user?.photo_url?.let { if (it.startsWith("http")) it else "https://bizo.aiko.qzz.io$it" }
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    name: String, 
    itemTitle: String, 
    lastMessage: String, 
    time: String, 
    unread: Boolean,
    avatarUrl: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(GraySurface),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null) {
                AsyncImage(model = avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = GrayText)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.weight(1f))
                Text(time, style = MaterialTheme.typography.labelSmall, color = GrayText)
            }
            Text(itemTitle, style = MaterialTheme.typography.labelSmall, color = GrayText, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Black))
        }
    }
}
