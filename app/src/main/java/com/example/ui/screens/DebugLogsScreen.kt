package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.data.*
import com.example.data.api.BizoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider

class DebugLogsViewModel(private val bizoService: BizoService) : ViewModel() {
    private val _history = MutableStateFlow<List<DebugLogHistoryItem>>(emptyList())
    val history: StateFlow<List<DebugLogHistoryItem>> = _history

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _sendResult = MutableStateFlow<SendDebugLogsResponse?>(null)
    val sendResult: StateFlow<SendDebugLogsResponse?> = _sendResult

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = bizoService.getDebugLogsHistory()
                _history.value = response.data
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.ERROR, "Erreur chargement historique logs", e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendLogs(logs: List<LogEntry>, context: Context) {
        if (logs.isEmpty()) return

        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }

        val appVersion = packageInfo?.versionName ?: "1.0.0-unknown"
        val appBuild = packageInfo?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                it.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                it.versionCode
            }
        } ?: 0

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = DebugLogsRequest(
                    app = AppInfo(version = appVersion, build = appBuild),
                    device = DeviceInfo(
                        model = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                        android = android.os.Build.VERSION.RELEASE
                    ),
                    context = LogContext(screen = "debug_logs"),
                    logs = logs
                )
                val response = bizoService.sendDebugLogs(request)
                _sendResult.value = response
                loadHistory()
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.ERROR, "Erreur envoi logs", e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSendResult() {
        _sendResult.value = null
    }
}

class DebugLogsViewModelFactory(private val bizoService: BizoService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DebugLogsViewModel(bizoService) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogsScreen(navController: NavController, bizoService: BizoService) {
    val context = LocalContext.current
    val viewModel: DebugLogsViewModel = viewModel(factory = DebugLogsViewModelFactory(bizoService))
    val logs by DebugLogger.logs.collectAsState()
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sendResult by viewModel.sendResult.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug & Logs", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { viewModel.sendLogs(logs, context) }, enabled = logs.isNotEmpty() && !isLoading) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Send, contentDescription = "Envoyer au serveur")
                        }
                        IconButton(onClick = {
                            val text = logs.joinToString("\n") { "[${it.timestamp}] [${it.category}] [${it.level}] ${it.title}: ${it.details ?: ""}" }
                            clipboardManager.setText(AnnotatedString(text))
                        }) {
                            Icon(Icons.Default.Info, contentDescription = "Copier logs")
                        }
                        IconButton(onClick = { DebugLogger.clear() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Effacer")
                        }
                    } else {
                        IconButton(onClick = { viewModel.loadHistory() }, enabled = !isLoading) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Logs") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) }, // Using Refresh as placeholder for History
                    label = { Text("Historique") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedTab == 0) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucun log disponible", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs) { entry ->
                            LogItem(entry)
                        }
                    }
                }
            } else {
                if (isLoading && history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (history.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucun historique d'envoi", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { item ->
                            HistoryItem(item)
                        }
                    }
                }
            }
        }
    }

    if (sendResult != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSendResult() },
            title = { Text("Logs envoyés") },
            text = {
                Column {
                    Text(sendResult!!.message)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Référence : ", fontWeight = FontWeight.Bold)
                    Text(sendResult!!.reference, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Date : ${sendResult!!.received_at}")
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSendResult() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun HistoryItem(item: DebugLogHistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ref: ${item.reference}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = item.received_at,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "${item.log_count} logs",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun LogItem(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.INFO -> Color.Blue
        LogLevel.WARN -> Color(0xFFFFA500) // Orange
        LogLevel.ERROR -> Color.Red
        LogLevel.SUCCESS -> Color(0xFF2E7D32) // Green
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = entry.level.name,
                        color = color,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.category.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = entry.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (!entry.details.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.details,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
