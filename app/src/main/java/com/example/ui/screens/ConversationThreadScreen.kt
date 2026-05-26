package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.data.ConversationResource
import com.example.data.DebugLogger
import com.example.data.InboxStateStore
import com.example.data.LogCategory
import com.example.data.MessageResource
import com.example.data.RealtimeEvent
import com.example.data.RealtimeManager
import com.example.data.SessionManager
import com.example.data.api.BizoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

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

    private val _onConversationCreated = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val onConversationCreated: SharedFlow<String> = _onConversationCreated

    init {
        if (!convIdInitial.startsWith("new_")) {
            loadConversation(convIdInitial)
            loadMessages(convIdInitial)
        } else {
            _isLoading.value = false
        }
    }

    private fun sortMessages(messages: List<MessageResource>): List<MessageResource> {
        return messages.sortedBy { it.created_at }
    }

    private fun addOrUpdateMessage(message: MessageResource) {
        val updated = _messages.value
            .filterNot { it.id == message.id }
            .plus(message)

        _messages.value = sortMessages(updated)
    }

    private fun loadConversation(id: String) {
        DebugLogger.info(LogCategory.CONVERSATION, "Chargement conversation", "ID: $id")
        viewModelScope.launch {
            try {
                val response = bizoService.getConversation(id)
                _conversation.value = response.data
                DebugLogger.success(LogCategory.CONVERSATION, "Conversation chargee", response.data.listing_title)
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.CONVERSATION, "Erreur chargement conversation", e.message)
            }
        }
    }

    private fun loadMessages(id: String) {
        DebugLogger.info(LogCategory.MESSAGE, "Chargement messages", "Conv: $id")
        viewModelScope.launch {
            try {
                val response = bizoService.getMessages(id)
                _messages.value = sortMessages(response.data)
                DebugLogger.success(LogCategory.MESSAGE, "Messages charges", "Count: ${response.data.size}")
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.MESSAGE, "Erreur chargement messages", e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onRealtimeMessage(message: MessageResource) {
        if (message.conv_id != _currentConvId.value) return

        addOrUpdateMessage(message)
        DebugLogger.info(LogCategory.MESSAGE, "Message injecte en temps reel", "Msg: ${message.id}")
    }

    fun sendMessage(text: String, type: String = "text") {
        val targetId = _currentConvId.value

        if (targetId.startsWith("new_")) {
            val listingId = targetId.removePrefix("new_").split("?").first()
            DebugLogger.info(LogCategory.CONVERSATION, "Creation conversation", "Listing: $listingId")

            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val response = bizoService.createConversation(
                        mapOf(
                            "listing_id" to listingId,
                            "message" to text
                        )
                    )

                    val realId = response.data.id
                    _currentConvId.value = realId
                    _conversation.value = response.data
                    _messages.value = sortMessages(listOf(response.message))
                    _onConversationCreated.tryEmit(realId)

                    DebugLogger.success(LogCategory.CONVERSATION, "Conversation creee", "ID: $realId")
                } catch (e: Exception) {
                    DebugLogger.error(LogCategory.CONVERSATION, "Erreur creation conversation", e.message)
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            DebugLogger.info(LogCategory.MESSAGE, "Envoi message", "Conv: $targetId")

            viewModelScope.launch {
                try {
                    val response = bizoService.sendMessage(
                        targetId,
                        mapOf(
                            "type" to type,
                            "text" to text
                        )
                    )

                    addOrUpdateMessage(response.data)
                    DebugLogger.success(LogCategory.MESSAGE, "Message envoye", "Msg: ${response.data.id}")
                } catch (e: Exception) {
                    DebugLogger.error(LogCategory.MESSAGE, "Erreur envoi message", e.message)
                }
            }
        }
    }
}

class ConversationThreadViewModelFactory(
    private val bizoService: BizoService,
    private val convId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ConversationThreadViewModel(bizoService, convId) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationThreadScreen(
    navController: NavController,
    bizoService: BizoService,
    convId: String,
    sessionManager: SessionManager,
    realtimeManager: RealtimeManager,
    inboxStore: InboxStateStore
) {
    val viewModel: ConversationThreadViewModel = viewModel(
        key = convId,
        factory = ConversationThreadViewModelFactory(bizoService, convId)
    )
    val messages by viewModel.messages.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentConvId by viewModel.currentConvId.collectAsState()
    val currentUserId = remember { sessionManager.getUserId() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(convId) {
        if (convId.contains("type=troc")) {
            messageText = "Je suis interesse par un troc pour cet article."
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.onConversationCreated.collect { newId ->
            navController.navigate("conversation/$newId") {
                popUpTo("conversation/$convId") { inclusive = true }
            }
        }
    }

    LaunchedEffect(currentConvId) {
        if (!currentConvId.startsWith("new_")) {
            inboxStore.setActiveConversation(currentConvId)
            inboxStore.markConversationRead(currentConvId)
            realtimeManager.subscribeToThread(currentConvId)
        }
    }

    DisposableEffect(currentConvId) {
        onDispose {
            inboxStore.setActiveConversation(null)
            realtimeManager.unsubscribeFromThread()
        }
    }

    LaunchedEffect(realtimeManager, currentConvId) {
        realtimeManager.events.collect { event ->
            if (event is RealtimeEvent.MessageCreated && event.message.conv_id == currentConvId) {
                viewModel.onRealtimeMessage(event.message)
                inboxStore.markConversationReadLocally(currentConvId)
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(imeBottom) {
        if (imeBottom > 0 && messages.isNotEmpty()) {
            delay(120)
            listState.scrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = conversation?.other_user?.display_name ?: "Conversation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        conversation?.let {
                            Text(
                                text = it.listing_title,
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
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            isMe = message.sender_id == currentUserId
                        )
                    }

                    if (convId.startsWith("new_") && messages.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Dites bonjour pour demarrer la conversation.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused && messages.isNotEmpty()) {
                                        scope.launch {
                                            delay(120)
                                            listState.scrollToItem(messages.lastIndex)
                                        }
                                    }
                                },
                            placeholder = { Text("Votre message...") },
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val trimmed = messageText.trim()
                                if (trimmed.isNotEmpty()) {
                                    viewModel.sendMessage(trimmed)
                                    messageText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageResource, isMe: Boolean) {
    val bubbleColor = if (isMe) Color(0xFF1F2937) else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val shape = if (isMe) {
        RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 1.dp
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.text.orEmpty(),
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Text(
            text = formatMessageTime(message.created_at),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (isMe) TextAlign.End else TextAlign.Start,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}

private fun formatMessageTime(raw: String): String {
    return runCatching {
        val normalized = raw.substringBefore('.').replace("T", " ")
        val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(normalized)
        SimpleDateFormat("HH:mm", Locale.FRANCE).format(parsed!!)
    }.getOrElse {
        ""
    }
}
