package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.ConversationResource
import com.example.data.DebugLogger
import com.example.data.InboxStateStore
import com.example.data.LogCategory
import com.example.data.MediaUrlResolver
import com.example.ui.components.BizoScreen
import com.example.ui.components.BizoStatePane
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MessagesScreen(
    navController: NavController,
    inboxStore: InboxStateStore
) {
    val conversations by inboxStore.conversations.collectAsState()

    LaunchedEffect(Unit) {
        inboxStore.setActiveConversation(null)
        if (conversations.isEmpty()) {
            inboxStore.refresh()
        }
    }

    BizoScreen(title = "Messages") { padding ->
        if (conversations.isEmpty()) {
            BizoStatePane(
                text = "Aucune conversation pour le moment.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding())
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    ConversationItem(conversation) {
                        DebugLogger.info(
                            LogCategory.NAV,
                            "Ouverture conversation",
                            "ID: ${conversation.id}, With: ${conversation.other_user.display_name}"
                        )
                        inboxStore.markConversationReadLocally(conversation.id)
                        navController.navigate("conversation/${conversation.id}")
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun ConversationItem(conversation: ConversationResource, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val otherUser = conversation.other_user
        val photoUrl = otherUser.photo_url
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = MediaUrlResolver.resolve(photoUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(otherUser.display_name.take(1).uppercase(), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = otherUser.display_name,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatConversationTime(conversation.last_message_at ?: conversation.created_at),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.last_message ?: "Aucun message",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (conversation.unread_count > 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unread_count > 0) {
                    Surface(shape = CircleShape, color = Color.Transparent) {
                        Badge {
                            Text(if (conversation.unread_count > 99) "99+" else conversation.unread_count.toString())
                        }
                    }
                }
            }
        }
    }
}

private fun formatConversationTime(raw: String): String {
    return runCatching {
        val normalized = raw.substringBefore('.').replace("T", " ")
        val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(normalized)
        SimpleDateFormat("HH:mm", Locale.FRANCE).format(parsed!!)
    }.getOrElse {
        raw.take(5)
    }
}
