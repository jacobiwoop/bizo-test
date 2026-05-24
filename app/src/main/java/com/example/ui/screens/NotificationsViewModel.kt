package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BizoService
import com.example.data.NotificationResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(private val bizoService: BizoService) : ViewModel() {
    private val _notifications = MutableStateFlow<List<NotificationResource>>(emptyList())
    val notifications: StateFlow<List<NotificationResource>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = bizoService.getNotifications()
                _notifications.value = response.data
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            try {
                bizoService.markNotificationRead(id)
                _notifications.value = _notifications.value.map {
                    if (it.id == id) it.copy(is_read = true) else it
                }
            } catch (e: Exception) {}
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                bizoService.markAllNotificationsRead()
                _notifications.value = _notifications.value.map { it.copy(is_read = true) }
            } catch (e: Exception) {}
        }
    }
}
