package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.*
import com.example.data.api.BizoService
import com.example.ui.components.BizoScreen
import com.example.ui.components.BizoStatePane
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val bizoService: BizoService) : ViewModel() {
    private val _listings = MutableStateFlow<List<ListingResource>>(emptyList())
    val listings: StateFlow<List<ListingResource>> = _listings

    init {
        loadListings()
    }

    fun loadListings() {
        DebugLogger.info(LogCategory.LISTING, "Chargement du flux d'accueil")
        viewModelScope.launch {
            try {
                val response = bizoService.getListings()
                _listings.value = response.data
                DebugLogger.success(LogCategory.LISTING, "Flux d'accueil chargé", "Count: ${response.data.size}")
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.LISTING, "Erreur chargement flux accueil", e.message)
                e.printStackTrace()
            }
        }
    }
}

class HomeViewModelFactory(
    private val bizoService: BizoService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(bizoService) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, bizoService: BizoService) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(bizoService))
    val listings by viewModel.listings.collectAsState()
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadListings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BizoScreen(
        title = "Bizo",
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("publish") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Publier")
            }
        }
    ) { padding ->
        if (listings.isEmpty()) {
            BizoStatePane(
                text = "Aucune annonce disponible pour le moment.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp
                ),
                modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())
            ) {
                items(listings) { listing ->
                    ListingCard(listing) {
                        DebugLogger.info(LogCategory.NAV, "Ouverture détail annonce", "ID: ${listing.id}, Title: ${listing.title}")
                        navController.navigate("item_detail/${listing.id}")
                    }
                }
            }
        }
    }
}

@Composable
fun ListingCard(listing: ListingResource, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            val photoUrl = listing.photos.firstOrNull()
            if (photoUrl != null) {
                val fullUrl = MediaUrlResolver.resolve(photoUrl)
                AsyncImage(
                    model = fullUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = listing.title, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    text = "${listing.price ?: 0} FCFA",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(text = listing.city, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
