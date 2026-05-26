package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.SessionManager
import com.example.data.api.BizoService
import com.example.data.InboxStateStore
import com.example.data.RealtimeManager
import com.example.ui.components.BizoScreen
import com.example.ui.screens.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BizoApp(
    sessionManager: SessionManager,
    bizoService: BizoService,
    realtimeManager: RealtimeManager,
    inboxStore: InboxStateStore
) {
    val navController = rememberNavController()
    val shellViewModel: AppShellViewModel = hiltViewModel()
    
    val items = listOf(
        Screen.Home,
        Screen.Messages,
        Screen.Profile
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val shellState by shellViewModel.state.collectAsStateWithLifecycle()
    val showBottomBar = items.any { it.route == currentDestination?.route }

    LaunchedEffect(shellState.authToken, currentDestination?.route) {
        val route = currentDestination?.route
        if (shellState.authToken == null && route != null && route !in listOf("splash", "auth")) {
            navController.navigate("auth") {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    BizoScreen(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                if (screen.route == Screen.Messages.route && shellState.unreadCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text(if (shellState.unreadCount > 99) "99+" else shellState.unreadCount.toString())
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
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable("splash") {
                SplashScreen(navController, sessionManager)
            }
            composable("auth") {
                AuthScreen(navController)
            }
            composable(Screen.Home.route) { 
                HomeScreen(navController) 
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
                PublishScreen(navController)
            }
            composable("edit_listing/{id}") { backStackEntry ->
                PublishScreen(navController)
            }
            composable("item_detail/{id}") { backStackEntry ->
                ItemDetailScreen(navController)
            }
            composable("conversation/{id}") { backStackEntry ->
                ConversationThreadScreen(navController, realtimeManager, inboxStore)
            }
        }
    }
}

sealed class Screen(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Home : Screen("home", Icons.Default.Home)
    object Messages : Screen("messages", Icons.Default.Email)
    object Profile : Screen("profile", Icons.Default.Person)
}
