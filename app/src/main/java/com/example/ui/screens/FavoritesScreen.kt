package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.*
import com.example.data.api.BizoService
import com.example.ui.components.BizoScreen
import com.example.ui.components.BizoStatePane
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FavoritesViewModel @Inject constructor(private val bizoService: BizoService) : ViewModel() {
    private val _favorites = MutableStateFlow<List<FavoriteResource>>(emptyList())
    val favorites: StateFlow<List<FavoriteResource>> = _favorites

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        DebugLogger.info(LogCategory.FAVORITE, "Chargement des favoris")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = bizoService.getFavorites()
                _favorites.value = response.data
                DebugLogger.success(LogCategory.FAVORITE, "Favoris chargés", "Count: ${response.data.size}")
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.FAVORITE, "Erreur chargement favoris", e.message)
                _error.value = "Impossible de charger les favoris."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeFavorite(listingId: String) {
        DebugLogger.info(LogCategory.FAVORITE, "Retrait du favori $listingId")
        viewModelScope.launch {
            try {
                bizoService.removeFavorite(listingId)
                _favorites.value = _favorites.value.filter { it.listing_id != listingId }
                DebugLogger.success(LogCategory.FAVORITE, "Favori retiré")
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.FAVORITE, "Erreur retrait favori", e.message)
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun FavoritesScreen(navController: NavController, bizoService: BizoService) {
    val viewModel: FavoritesViewModel = hiltViewModel()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    BizoScreen(title = "Mes favoris", onBack = { navController.popBackStack() }) { padding ->
        if (isLoading) {
            BizoStatePane("Chargement...", modifier = Modifier.padding(padding), loading = true)
        } else if (error != null && favorites.isEmpty()) {
            BizoStatePane(error!!, modifier = Modifier.padding(padding))
        } else if (favorites.isEmpty()) {
            BizoStatePane("Vous n'avez pas encore de favoris.", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                contentPadding = PaddingValues(
                    bottom = padding.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favorites) { favorite ->
                    FavoriteItemView(
                        favorite = favorite,
                        onClick = {
                            DebugLogger.info(LogCategory.NAV, "Ouverture détail depuis favoris", "ID: ${favorite.listing_id}")
                            navController.navigate("item_detail/${favorite.listing_id}")
                        },
                        onRemove = {
                            viewModel.removeFavorite(favorite.listing_id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteItemView(favorite: FavoriteResource, onClick: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val photoUrl = favorite.listing_photo
            val fullUrl = MediaUrlResolver.resolve(photoUrl)

            AsyncImage(
                model = fullUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = favorite.listing_title ?: "Annonce sans titre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    text = "Ajouté le ${favorite.created_at.split("T").first()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Retirer", tint = Color.Red)
            }
        }
    }
}
