package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.*
import com.example.data.api.BizoService
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ItemDetailViewModel(
    private val bizoService: BizoService,
    private val id: String
) : ViewModel() {
    private val _listing = MutableStateFlow<ListingResource?>(null)
    val listing: StateFlow<ListingResource?> = _listing

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isFavorited = MutableStateFlow(false)
    val isFavorited: StateFlow<Boolean> = _isFavorited

    init {
        loadListing()
        checkFavoriteStatus()
    }

    private fun loadListing() {
        viewModelScope.launch {
            try {
                val response = bizoService.getListing(id)
                _listing.value = response.data
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun checkFavoriteStatus() {
        viewModelScope.launch {
            try {
                val response = bizoService.getFavorites()
                _isFavorited.value = response.data.any { it.listing_id == id }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun toggleFavorite() {
        val current = _isFavorited.value
        viewModelScope.launch {
            try {
                if (current) bizoService.removeFavorite(id)
                else bizoService.addFavorite(id)
                _isFavorited.value = !current
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteListing(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                bizoService.deleteListing(id)
                onSuccess()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    navController: NavController,
    bizoService: BizoService,
    id: String,
    sessionManager: SessionManager
) {
    val viewModel = remember { ItemDetailViewModel(bizoService, id) }
    val listing by viewModel.listing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserId = sessionManager.getUserId()
    
    var showFullScreenPager by remember { mutableStateOf(false) }
    var selectedPhotoIndex by remember { mutableStateOf(0) }

    val isFavorited by viewModel.isFavorited.collectAsState()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (listing == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Annonce introuvable")
        }
    } else {
        val item = listing!!
        val isOwner = item.owner?.id == currentUserId
        val photos = if (item.photos.isEmpty()) listOf("https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400") else item.photos

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorited) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { /* Share logic */ }) { Icon(Icons.Default.Share, null) }
                    }
                )
            },
            bottomBar = {
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isOwner) {
                            PrimaryButton(
                                text = "Modifier",
                                onClick = { /* TODO: navigation */ },
                                modifier = Modifier.weight(1f)
                            )
                            SecondaryButton(
                                text = "Supprimer",
                                onClick = { 
                                    viewModel.deleteListing {
                                        navController.popBackStack()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            PrimaryButton(
                                text = "Contacter",
                                onClick = {
                                    navController.navigate("conversation/new_${item.id}")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SecondaryButton(
                                text = "Troc",
                                onClick = {
                                    navController.navigate("conversation/new_${item.id}?type=troc")
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Galerie
                item {
                    val pagerState = rememberPagerState(pageCount = { photos.size })
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { index ->
                            val photoUrl = photos[index]
                            val fullUrl = if (photoUrl.startsWith("http")) photoUrl else "https://bizo.aiko.qzz.io$photoUrl"
                            AsyncImage(
                                model = fullUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clickable {
                                    selectedPhotoIndex = index
                                    showFullScreenPager = true
                                },
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        // Indicateur de page
                        if (photos.size > 1) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = CircleShape
                             ) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${photos.size}",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                
                // Infos de base
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = item.category.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "${item.price ?: 0} FCFA",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "${item.city}${if (item.neighborhood != null) ", ${item.neighborhood}" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
                
                // Vendeur
                item {
                    val owner = item.owner
                    if (owner != null) {
                        Surface(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                val avatarUrl = owner.photo_url
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (avatarUrl != null) {
                                        val fullAvatarUrl = if (avatarUrl.startsWith("http")) avatarUrl else "https://bizo.aiko.qzz.io$avatarUrl"
                                        AsyncImage(fullAvatarUrl, null, contentScale = ContentScale.Crop)
                                    } else {
                                        Text(owner.display_name.take(1))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(owner.display_name, fontWeight = FontWeight.Bold)
                                    Text("Note: ${owner.rating ?: 0.0} (★)", style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = { /* TODO */ }) {
                                    Text("Voir profil")
                                }
                            }
                        }
                    }
                }
                
                // Description
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Description", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.description, style = MaterialTheme.typography.bodyLarge)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Caractéristiques
                        Row(modifier = Modifier.fillMaxWidth()) {
                            SpecBox(title = "ÉTAT", value = item.condition, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(12.dp))
                            SpecBox(title = "LIVRAISON", value = item.delivery_mode, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    
    // Galerie Plein Écran
    if (showFullScreenPager && listing != null) {
        val photos = if (listing!!.photos.isEmpty()) listOf("https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400") else listing!!.photos
        Dialog(
            onDismissRequest = { showFullScreenPager = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                val pagerState = rememberPagerState(initialPage = selectedPhotoIndex, pageCount = { photos.size })
                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                        val photoUrl = photos[index]
                        val fullUrl = if (photoUrl.startsWith("http")) photoUrl else "https://bizo.aiko.qzz.io$photoUrl"
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    IconButton(
                        onClick = { showFullScreenPager = false },
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding()
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SpecBox(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value.replace("_", " ").uppercase(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}
