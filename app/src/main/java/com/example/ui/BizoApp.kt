package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.SessionManager
import com.example.data.api.BizoService
import com.example.ui.screens.*

@Composable
fun BizoApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = SessionManager(context)
    val bizoService = BizoService.create(sessionManager)
    
    val items = listOf(
        Screen.Home,
        Screen.Messages,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = items.any { it.route == currentDestination?.route }
            
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = null) },
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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { 
                HomeScreen(navController, bizoService) 
            }
            composable(Screen.Messages.route) { 
                MessagesScreen(navController, bizoService) 
            }
            composable(Screen.Profile.route) { 
                ProfileScreen(navController, bizoService, sessionManager) 
            }
            composable("item_detail/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")!!
                ItemDetailScreen(navController, bizoService, id, sessionManager)
            }
            composable("conversation/{id}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")!!
                ConversationThreadScreen(navController, bizoService, id, sessionManager)
            }
        }
    }
}

sealed class Screen(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Home : Screen("home", Icons.Default.Home)
    object Messages : Screen("messages", Icons.Default.Email)
    object Profile : Screen("profile", Icons.Default.Person)
}
