package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

enum class LogLevel { INFO, WARN, ERROR, SUCCESS }
enum class LogCategory { AUTH, PROFILE, LISTING, FAVORITE, CONVERSATION, MESSAGE, NAV, ERROR }

data class LogEntry(
    val timestamp: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()),
    val level: LogLevel,
    val category: LogCategory,
    val title: String,
    val details: String? = null
)

data class SendDebugLogsResponse(
    val message: String,
    val reference: String,
    val received_at: String
)

data class DebugLogHistoryItem(
    val reference: String,
    val received_at: String,
    val log_count: Int,
    val file: String
)

object DebugLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs

    private const val MAX_LOGS = 500

    fun log(level: LogLevel, category: LogCategory, title: String, details: String? = null) {
        val entry = LogEntry(level = level, category = category, title = title, details = details)
        val currentList = _logs.value.toMutableList()
        currentList.add(0, entry)
        if (currentList.size > MAX_LOGS) {
            currentList.removeAt(currentList.size - 1)
        }
        _logs.value = currentList
        
        // Print to logcat as well for dev convenience
        println("[$category] [${level.name}] $title ${details ?: ""}")
    }

    fun info(category: LogCategory, title: String, details: String? = null) = log(LogLevel.INFO, category, title, details)
    fun warn(category: LogCategory, title: String, details: String? = null) = log(LogLevel.WARN, category, title, details)
    fun error(category: LogCategory, title: String, details: String? = null) = log(LogLevel.ERROR, category, title, details)
    fun success(category: LogCategory, title: String, details: String? = null) = log(LogLevel.SUCCESS, category, title, details)

    fun clear() {
        _logs.value = emptyList()
    }
}
