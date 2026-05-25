package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.*
import com.example.data.api.BizoService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConversationThreadViewModel(
    private val bizoService: BizoService,
    private val convIdInitial: String
) : ViewModel() {
    private val _messages = MutableStateFlow<List<MessageResource>>(emptyList())
    val messages: StateFlow<List<MessageResource>> = _messages
    
    private val _conversation = MutableStateFlow<ConversationResource?>(null)
    val conversation: StateFlow<ConversationResource?> = _conversation

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentConvId = MutableStateFlow(convIdInitial)
    val currentConvId: StateFlow<String> = _currentConvId

    private val _onConversationCreated = MutableStateFlow<String?>(null)
    val onConversationCreated: StateFlow<String?> = _onConversationCreated

    init {
        if (!convIdInitial.startsWith("new_")) {
            loadMessages(convIdInitial)
            loadConversation(convIdInitial)
        } else {
            _isLoading.value = false
        }
    }

    private fun loadConversation(id: String) {
        viewModelScope.launch {
            try {
                val response = bizoService.getConversation(id)
                _conversation.value = response.data
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun loadMessages(id: String) {
        viewModelScope.launch {
            try {
                val response = bizoService.getMessages(id)
                _messages.value = response.data.reversed()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(text: String, type: String = "text") {
        val targetId = _currentConvId.value
        if (targetId.startsWith("new_")) {
            val partAfterNew = targetId.removePrefix("new_")
            val listingId = partAfterNew.split("?").first()
            
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val body = mapOf("listing_id" to listingId, "message" to text)
                    val response = bizoService.createConversation(body)
                    
                    val realId = response.data.id
                    _currentConvId.value = realId
                    _conversation.value = response.data
                    _messages.value = listOf(response.message)
                    _onConversationCreated.value = realId
                    
                    loadMessages(realId)
                } catch (e: Exception) { 
                    e.printStackTrace() 
                    _isLoading.value = false
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    val body = mapOf("type" to type, "text" to text)
                    val response = bizoService.sendMessage(targetId, body)
                    // On injecte immédiatement le message de retour pour l'UX (ordre récent en premier)
                    _messages.value = listOf(response.data) + _messages.value
                    // On synchronise avec le reload au cas où
                    loadMessages(targetId)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationThreadScreen(
    navController: NavController,
    bizoService: BizoService,
    convId: String,
    sessionManager: SessionManager
) {
    val viewModel = remember { ConversationThreadViewModel(bizoService, convId) }
    val messages by viewModel.messages.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserId = sessionManager.getUserId()
    
    // On extrait le type depuis la route (si on passait des arguments riches ce serait mieux)
    // Ici on simplifie en initialisant messageText si c'est un troc
    val onConvCreated by viewModel.onConversationCreated.collectAsState()
    
    LaunchedEffect(onConvCreated) {
        onConvCreated?.let { newId ->
            // On remplace la route actuelle par la vraie pour éviter de revenir sur "new_..." au back
            navController.navigate("conversation/$newId") {
                popUpTo("conversation/$convId") { inclusive = true }
            }
        }
    }

    var messageText by remember { mutableStateOf("") }
    
    // Effet initial pour le troc
    LaunchedEffect(convId) {
        if (convId.contains("type=troc")) {
            messageText = "Je suis intéressé par un troc pour cet article."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = conversation?.other_user?.display_name ?: "Conversation",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (conversation != null) {
                            Text(
                                text = conversation!!.listing_title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.imePadding() // Gestion du clavier
            ) {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Votre message...") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                reverseLayout = true, // Afficher les derniers messages en bas
                contentPadding = PaddingValues(16.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message, isMe = message.sender_id == currentUserId)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (convId.startsWith("new_") && messages.isEmpty()) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                            Text("Dites bonjour pour démarrer la conversation !", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageResource, isMe: Boolean) {
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val shape = if (isMe) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 1.dp
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text ?: "",
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
