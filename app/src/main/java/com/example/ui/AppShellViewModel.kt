package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ConversationResource
import com.example.data.InboxStateStore
import com.example.data.RealtimeEvent
import com.example.data.RealtimeManager
import com.example.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppShellState(
    val authToken: String? = null,
    val userId: String? = null,
    val conversations: List<ConversationResource> = emptyList()
) {
    val unreadCount: Int
        get() = conversations.sumOf { it.unread_count }
}

@HiltViewModel
class AppShellViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val realtimeManager: RealtimeManager,
    val inboxStore: InboxStateStore
) : ViewModel() {

    val state: StateFlow<AppShellState> = combine(
        sessionManager.authToken,
        sessionManager.userId,
        inboxStore.conversations
    ) { authToken, userId, conversations ->
        AppShellState(
            authToken = authToken,
            userId = userId,
            conversations = conversations
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppShellState()
    )

    init {
        viewModelScope.launch {
            sessionManager.userId.collect { userId ->
                if (userId != null) {
                    inboxStore.refresh()
                    realtimeManager.subscribeToInbox(userId)
                } else {
                    inboxStore.clear()
                    realtimeManager.unsubscribeFromInbox()
                }
            }
        }

        viewModelScope.launch {
            realtimeManager.events.collect { event ->
                if (event is RealtimeEvent.ConversationSummaryUpdated) {
                    inboxStore.upsertConversation(event.conversation)
                }
            }
        }
    }
}
