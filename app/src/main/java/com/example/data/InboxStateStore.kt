package com.example.data

import com.example.data.api.BizoService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InboxStateStore(
    private val bizoService: BizoService
) {
    private val mutex = Mutex()
    private val _conversations = MutableStateFlow<List<ConversationResource>>(emptyList())
    val conversations: StateFlow<List<ConversationResource>> = _conversations.asStateFlow()

    @Volatile
    private var activeConversationId: String? = null

    suspend fun refresh() {
        DebugLogger.info(LogCategory.CONVERSATION, "Rafraichissement inbox")
        try {
            val response = bizoService.getConversations()
            mutex.withLock {
                _conversations.value = sortConversations(response.data.map(::sanitizeConversation))
            }
            DebugLogger.success(
                LogCategory.CONVERSATION,
                "Inbox rafraichie",
                "Count: ${response.data.size}"
            )
        } catch (e: Exception) {
            DebugLogger.error(LogCategory.CONVERSATION, "Erreur refresh inbox", e.message)
        }
    }

    fun clear() {
        activeConversationId = null
        _conversations.value = emptyList()
    }

    fun setActiveConversation(conversationId: String?) {
        activeConversationId = conversationId
        if (conversationId != null) {
            markConversationReadLocally(conversationId)
        }
    }

    fun upsertConversation(conversation: ConversationResource) {
        val sanitized = sanitizeConversation(conversation)
        val updated = _conversations.value
            .filterNot { it.id == sanitized.id }
            .plus(sanitized)

        _conversations.value = sortConversations(updated)
    }

    fun markConversationReadLocally(conversationId: String) {
        _conversations.value = _conversations.value.map { conversation ->
            if (conversation.id == conversationId && conversation.unread_count != 0) {
                conversation.copy(unread_count = 0)
            } else {
                conversation
            }
        }
    }

    suspend fun markConversationRead(conversationId: String) {
        markConversationReadLocally(conversationId)
        try {
            bizoService.markConversationRead(conversationId)
            DebugLogger.success(LogCategory.CONVERSATION, "Conversation marquee lue", conversationId)
        } catch (e: Exception) {
            DebugLogger.error(LogCategory.CONVERSATION, "Erreur marquage lecture", e.message)
        }
    }

    private fun sanitizeConversation(conversation: ConversationResource): ConversationResource {
        return if (conversation.id == activeConversationId && conversation.unread_count > 0) {
            conversation.copy(unread_count = 0)
        } else {
            conversation
        }
    }

    private fun sortConversations(conversations: List<ConversationResource>): List<ConversationResource> {
        return conversations.sortedByDescending { it.last_message_at ?: it.created_at }
    }
}
