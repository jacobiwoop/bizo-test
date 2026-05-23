package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ItemDetailScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PublishScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.Black
import com.example.ui.theme.GrayText

sealed class Route(val path: String) {
    object Splash : Route("splash")
    object Onboarding : Route("onboarding")
    object Auth : Route("auth")
    object Home : Route("home")
    object Detail : Route("detail/{itemId}") {
        fun createRoute(itemId: String) = "detail/$itemId"
    }
    object Publish : Route("publish")
    object Messages : Route("messages")
    object Profile : Route("profile")
}

@Composable
fun BizoApp() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val bottomBarRoutes = listOf(Route.Home.path, Route.Messages.path, Route.Profile.path)
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                ) {
                    NavigationBarItem(
                        selected = currentRoute == Route.Home.path,
                        onClick = {
                            navController.navigate(Route.Home.path) {
                                popUpTo(Route.Home.path) { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Black,
                            selectedTextColor = Black,
                            unselectedIconColor = GrayText,
                            unselectedTextColor = GrayText,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == Route.Messages.path,
                        onClick = {
                            navController.navigate(Route.Messages.path) {
                                popUpTo(Route.Home.path)
                            }
                        },
                        icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Messages") },
                        label = { Text("Messages") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Black,
                            selectedTextColor = Black,
                            unselectedIconColor = GrayText,
                            unselectedTextColor = GrayText,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == Route.Profile.path,
                        onClick = {
                            navController.navigate(Route.Profile.path) {
                                popUpTo(Route.Home.path)
                            }
                        },
                        icon = { Icon(Icons.Default.PersonOutline, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Black,
                            selectedTextColor = Black,
                            unselectedIconColor = GrayText,
                            unselectedTextColor = GrayText,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Route.Home.path) {
                FloatingActionButton(
                    onClick = { navController.navigate(Route.Publish.path) },
                    containerColor = Black,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Publier")
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Route.Splash.path,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Route.Splash.path) {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Route.Home.path) {
                            popUpTo(Route.Splash.path) { inclusive = true }
                        }
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(Route.Onboarding.path) {
                            popUpTo(Route.Splash.path) { inclusive = true }
                        }
                    }
                )
            }
            composable(Route.Onboarding.path) {
                OnboardingScreen(
                    onStart = {
                        navController.navigate(Route.Auth.path) {
                            popUpTo(Route.Onboarding.path) { inclusive = true }
                        }
                    },
                    onLogin = {
                        navController.navigate(Route.Auth.path) {
                            popUpTo(Route.Onboarding.path) { inclusive = true }
                        }
                    }
                )
            }
            composable(Route.Auth.path) {
                AuthScreen(onAuthenticated = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Auth.path) { inclusive = true }
                    }
                })
            }
            composable(Route.Home.path) {
                HomeScreen(onItemClick = { itemId ->
                    navController.navigate(Route.Detail.createRoute(itemId))
                })
            }
            composable(Route.Detail.path) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                ItemDetailScreen(
                    itemId = itemId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Route.Publish.path) {
                PublishScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.Messages.path) {
                MessagesScreen()
            }
            composable(Route.Profile.path) {
                ProfileScreen()
            }
        }
    }
}
