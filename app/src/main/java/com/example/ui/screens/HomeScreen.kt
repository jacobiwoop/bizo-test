package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.data.DebugLogger
import com.example.data.ListingResource
import com.example.data.LogCategory
import com.example.ui.components.BizoListingCard
import com.example.ui.components.BizoStatePane
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(private val bizoService: com.example.data.api.BizoService) : ViewModel() {
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

private data class HomeCategory(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

private val homeCategories = listOf(
    HomeCategory("vehicules", "Vehicules", Icons.Default.DirectionsCar),
    HomeCategory("maison", "Maison", Icons.Default.Apartment),
    HomeCategory("electronique", "Electronique", Icons.Default.Devices),
    HomeCategory("vetements", "Mode", Icons.Default.Checkroom),
    HomeCategory("services", "Services", Icons.Default.Handyman),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeViewModel = hiltViewModel()
    val listings by viewModel.listings.collectAsStateWithLifecycle()
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadListings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val quickSearches = remember(listings) {
        listings
            .map { it.title.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(6)
    }

    val filteredListings = remember(listings, searchQuery, selectedCategory) {
        listings.filter { listing ->
            val matchesCategory = selectedCategory == null || listing.category.equals(selectedCategory, ignoreCase = true)
            val query = searchQuery.trim()
            val matchesSearch = query.isEmpty() ||
                listing.title.contains(query, ignoreCase = true) ||
                listing.description.contains(query, ignoreCase = true) ||
                listing.city.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (listings.isEmpty()) {
            BizoStatePane(
                text = "Aucune annonce disponible pour le moment.",
                modifier = Modifier.fillMaxSize()
            )
            return@Box
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 112.dp)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                HomeHeroSection(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    quickSearches = quickSearches,
                    onQuickSearch = { searchQuery = it },
                    onFavorites = { navController.navigate("favorites") },
                    onMessages = { navController.navigate("messages") }
                )
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                CategoryPanel(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { key ->
                        selectedCategory = if (selectedCategory == key) null else key
                    }
                )
            }

            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Annonces populaires",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedCategory == null) "Ce qui bouge autour de toi" else "Filtre actif : ${selectedCategory!!.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { selectedCategory = null; searchQuery = "" }) {
                        Text("Tout voir")
                    }
                }
            }

            if (filteredListings.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    ElevatedCard(
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Aucun résultat",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Essaie une autre recherche ou retire un filtre catégorie.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredListings, key = { it.id }) { listing ->
                    BizoListingCard(
                        listing = listing,
                        modifier = Modifier,
                        onClick = {
                            DebugLogger.info(LogCategory.NAV, "Ouverture détail annonce", "ID: ${listing.id}, Title: ${listing.title}")
                            navController.navigate("item_detail/${listing.id}")
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate("publish") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp)
                .size(72.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Publier", modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun HomeHeroSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    quickSearches: List<String>,
    onQuickSearch: (String) -> Unit,
    onFavorites: () -> Unit,
    onMessages: () -> Unit,
) {
    val headerColor = MaterialTheme.colorScheme.onSurface
    val searchBackground = MaterialTheme.colorScheme.surface
    val chipBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(headerColor)
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Explorer",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Trouve la bonne annonce",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HeaderCircleButton(icon = Icons.Default.FavoriteBorder, onClick = onFavorites)
                    HeaderCircleButton(icon = Icons.Default.MoreHoriz, onClick = onMessages)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Recherche une annonce") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = searchBackground,
                    unfocusedContainerColor = searchBackground,
                    disabledContainerColor = searchBackground,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            if (quickSearches.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quickSearches.forEach { quickSearch ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = chipBackground,
                            modifier = Modifier.clickable { onQuickSearch(quickSearch) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = quickSearch,
                                    color = MaterialTheme.colorScheme.surface,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
    ) {
        Box(
            modifier = Modifier.size(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPanel(
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Bizo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 3
            ) {
                homeCategories.forEach { category ->
                    val selected = selectedCategory == category.key
                    CategoryTile(
                        label = category.label,
                        icon = category.icon,
                        selected = selected,
                        onClick = { onCategorySelected(category.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val iconColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .background(background)
            .padding(horizontal = 10.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
