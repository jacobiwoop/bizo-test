package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.*
import com.example.data.api.BizoService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val bizoService: BizoService,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _user = MutableStateFlow<UserResource?>(null)
    val user: StateFlow<UserResource?> = _user

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
                val response = bizoService.getProfile()
                _user.value = response.data
                sessionManager.saveUser(response.data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun ProfileScreen(
    navController: NavController,
    bizoService: BizoService,
    sessionManager: SessionManager
) {
    val viewModel = remember { ProfileViewModel(bizoService, sessionManager) }
    val user by viewModel.user.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val avatarUrl = user?.photo_url
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl != null) {
                        val fullUrl = if (avatarUrl.startsWith("http")) avatarUrl else "https://bizo.aiko.qzz.io$avatarUrl"
                        AsyncImage(fullUrl, null, contentScale = ContentScale.Crop)
                    } else {
                        Text(user?.display_name?.take(1) ?: "?", style = MaterialTheme.typography.displayMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = user?.display_name ?: "Chargement...",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "@${user?.username ?: "bizo_user"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                if (!user?.bio.isNullOrBlank()) {
                    Text(
                        text = user?.bio!!,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                        color = Color.DarkGray
                    )
                }
            }
        }
        
        // Statistiques
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Note", value = "${user?.rating ?: 0.0} ★")
                StatItem(label = "Ventes", value = "${user?.total_sales ?: 0}")
                StatItem(label = "Avis", value = "${user?.review_count ?: 0}")
            }
        }
        
        // Actions
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Mon Compte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                
                ProfileOption(Icons.Default.ShoppingCart, "Mes annonces") {
                    // navController.navigate("my_listings")
                }
                ProfileOption(Icons.Default.Favorite, "Mes favoris") {
                    // navController.navigate("favorites")
                }
                ProfileOption(Icons.Default.Edit, "Modifier le profil") {
                    // navController.navigate("edit_profile")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Paramètres", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                
                ProfileOption(Icons.Default.Notifications, "Notifications") {}
                ProfileOption(Icons.Default.Lock, "Sécurité") {}
                ProfileOption(Icons.Default.Info, "Aide & Support") {}
                
                Spacer(modifier = Modifier.height(32.dp))
                
                TextButton(
                    onClick = {
                        sessionManager.clearSession()
                        // navController.navigate("auth") { popUpTo(0) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.ExitToApp, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Déconnexion")
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
fun ProfileOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}
