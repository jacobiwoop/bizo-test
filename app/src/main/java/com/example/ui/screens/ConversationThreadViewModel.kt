package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BizoService
import com.example.data.MessageResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConversationState(
    val messages: List<MessageResource> = emptyList(),
    val isLoading: Boolean = false,
    val currentUserId: String? = null
)

class ConversationThreadViewModel(
    private val convId: String,
    private val bizoService: BizoService
) : ViewModel() {
    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    init {
        loadMessages()
        fetchCurrentUser()
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            try {
                val profile = bizoService.getProfile()
                _state.value = _state.value.copy(currentUserId = profile.id)
            } catch (e: Exception) {}
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val response = bizoService.getMessages(convId)
                _state.value = _state.value.copy(messages = response.data, isLoading = false)
                bizoService.markRead(convId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                val response = bizoService.sendTextMessage(convId, text)
                val newMessage = response.data
                val updatedMessages = _state.value.messages + newMessage
                _state.value = _state.value.copy(messages = updatedMessages)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
