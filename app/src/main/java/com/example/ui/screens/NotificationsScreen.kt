package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.Dependencies
import com.example.data.NotificationResource
import com.example.ui.theme.Black
import com.example.ui.theme.GraySurface
import com.example.ui.theme.GrayText
import com.example.ui.theme.White

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: NotificationsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotificationsViewModel(Dependencies.getBizoService(context)) as T
        }
    })
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(White)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Black)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Black)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { viewModel.markAllRead() }) {
                Text("Tout lire", color = Black, fontWeight = FontWeight.Bold)
            }
        }

        if (isLoading && notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Black)
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune notification", color = GrayText)
            }
        } else {
            LazyColumn {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = { viewModel.markRead(notification.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: NotificationResource, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notification.is_read) White else GraySurface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(if (notification.is_read) GraySurface else Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when(notification.type) {
                    "new_message" -> "💬"
                    "new_favorite" -> "❤️"
                    "transaction_done" -> "🤝"
                    else -> "🔔"
                },
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(notification.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Black)
            Text(notification.body, style = MaterialTheme.typography.bodyMedium, color = GrayText)
            Spacer(modifier = Modifier.height(4.dp))
            Text("il y a quelques instants", style = MaterialTheme.typography.labelSmall, color = GrayText)
        }
        if (!notification.is_read) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Black))
        }
    }
}
