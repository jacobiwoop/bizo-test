package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.InboxStateStore
import com.example.data.RealtimeEvent
import com.example.data.RealtimeManager
import com.example.data.SessionManager
import com.example.data.api.BizoService
import com.example.ui.screens.*
import kotlinx.coroutines.flow.collect

@Composable
fun BizoApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val bizoService = remember { BizoService.create(sessionManager) }
    val realtimeManager = remember { RealtimeManager(sessionManager) }
    val inboxStore = remember { InboxStateStore(bizoService) }
    
    val items = listOf(
        Screen.Home,
        Screen.Messages,
        Screen.Profile
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val authToken by sessionManager.authToken.collectAsState()
    val userId by sessionManager.userId.collectAsState()
    val inboxConversations by inboxStore.conversations.collectAsState()
    val unreadCount = inboxConversations.sumOf { it.unread_count }
    val showBottomBar = items.any { it.route == currentDestination?.route }

    LaunchedEffect(userId) {
        if (userId != null) {
            inboxStore.refresh()
            realtimeManager.subscribeToInbox(userId!!)
        } else {
            inboxStore.clear()
            realtimeManager.unsubscribeFromInbox()
        }
    }

    LaunchedEffect(realtimeManager) {
        realtimeManager.events.collect { event ->
            if (event is RealtimeEvent.ConversationSummaryUpdated) {
                inboxStore.upsertConversation(event.conversation)
            }
        }
    }

    LaunchedEffect(authToken, currentDestination?.route) {
        val route = currentDestination?.route
        if (authToken == null && route != null && route !in listOf("splash", "auth")) {
            navController.navigate("auth") {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                if (screen.route == Screen.Messages.route && unreadCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                                            }
                                        }
                                    ) {
                                        Icon(screen.icon!!, contentDescription = null)
                                    }
                                } else {
                                    Icon(screen.icon!!, contentDescription = null)
                                }
                            },
                            label = { Text(screen.route.replaceFirstChar { it.uppercase() }) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(
                bottom = innerPadding.calculateBottomPadding()
            )
        ) {
            composable("splash") {
                SplashScreen(navController, sessionManager)
            }
            composable("auth") {
                AuthScreen(navController, bizoService, sessionManager)
            }
            composable(Screen.Home.route) { 
                HomeScreen(navController, bizoService) 
            }
            composable(Screen.Messages.route) { 
                MessagesScreen(navController, inboxStore) 
            }
            composable(Screen.Profile.route) { 
                ProfileScreen(navController, bizoService, sessionManager) 
            }
            composable("debug_logs") {
                DebugLogsScreen(navController, bizoService)
            }
            composable("my_listings") {
                MyListingsScreen(navController, bizoService)
            }
            composable("favorites") {
                FavoritesScreen(navController, bizoService)
            }
            composable("publish") {
                PublishScreen(navController, bizoService)
            }
            composable("edit_listing/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                PublishScreen(navController, bizoService, id)
            }
            composable("item_detail/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")!!
                ItemDetailScreen(navController, bizoService, id, sessionManager)
            }
            composable("conversation/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")!!
                ConversationThreadScreen(navController, bizoService, id, sessionManager, realtimeManager, inboxStore)
            }
        }
    }
}

sealed class Screen(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Home : Screen("home", Icons.Default.Home)
    object Messages : Screen("messages", Icons.Default.Email)
    object Profile : Screen("profile", Icons.Default.Person)
}
