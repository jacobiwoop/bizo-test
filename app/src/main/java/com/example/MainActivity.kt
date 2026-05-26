package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.InboxStateStore
import com.example.data.RealtimeManager
import com.example.data.SessionManager
import com.example.data.api.BizoService
import com.example.ui.BizoApp
import com.example.ui.theme.BizoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var bizoService: BizoService
    @Inject lateinit var realtimeManager: RealtimeManager
    @Inject lateinit var inboxStore: InboxStateStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BizoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BizoApp(
                        sessionManager = sessionManager,
                        bizoService = bizoService,
                        realtimeManager = realtimeManager,
                        inboxStore = inboxStore
                    )
                }
            }
        }
    }
}
