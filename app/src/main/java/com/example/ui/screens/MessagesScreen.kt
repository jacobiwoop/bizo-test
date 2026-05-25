package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.*
import com.example.data.api.BizoService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MessagesViewModel(private val bizoService: BizoService) : ViewModel() {
    private val _conversations = MutableStateFlow<List<ConversationResource>>(emptyList())
    val conversations: StateFlow<List<ConversationResource>> = _conversations

    init {
        loadConversations()
    }

    fun loadConversations() {
        DebugLogger.info(LogCategory.CONVERSATION, "Chargement des conversations")
        viewModelScope.launch {
            try {
                val response = bizoService.getConversations()
                _conversations.value = sortConversations(response.data)
                DebugLogger.success(LogCategory.CONVERSATION, "Conversations chargées", "Count: ${response.data.size}")
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.CONVERSATION, "Erreur chargement conversations", e.message)
                e.printStackTrace()
            }
        }
    }

    fun upsertConversation(conversation: ConversationResource) {
        val updated = _conversations.value
            .filterNot { it.id == conversation.id }
            .plus(conversation)

        _conversations.value = sortConversations(updated)
    }

    private fun sortConversations(conversations: List<ConversationResource>): List<ConversationResource> {
        return conversations.sortedByDescending { it.last_message_at ?: it.created_at }
    }
}

class MessagesViewModelFactory(
    private val bizoService: BizoService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MessagesViewModel(bizoService) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    navController: NavController,
    bizoService: BizoService,
    realtimeManager: RealtimeManager
) {
    val viewModel: MessagesViewModel = viewModel(factory = MessagesViewModelFactory(bizoService))
    val conversations by viewModel.conversations.collectAsState()

    LaunchedEffect(realtimeManager) {
        realtimeManager.events.collectLatest { event ->
            if (event is RealtimeEvent.ConversationSummaryUpdated) {
                viewModel.upsertConversation(event.conversation)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding())
        ) {
            items(conversations) { conversation ->
                ConversationItem(conversation) {
                    DebugLogger.info(LogCategory.NAV, "Ouverture conversation", "ID: ${conversation.id}, With: ${conversation.other_user.display_name}")
                    navController.navigate("conversation/${conversation.id}")
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val otherUser = conversation.other_user
        val photoUrl = otherUser.photo_url
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                val fullUrl = if (photoUrl.startsWith("http")) photoUrl else "https://bizo.aiko.qzz.io$photoUrl"
                AsyncImage(
                    model = fullUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(otherUser.display_name.take(1))
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = otherUser.display_name, fontWeight = FontWeight.Bold)
            Text(
                text = conversation.last_message ?: "Aucun message",
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                color = if (conversation.unread_count > 0) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }
        
        if (conversation.unread_count > 0) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(12.dp)
            ) {}
        }
    }
}
