package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.components.BizoStatePane
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val bizoService: com.example.data.api.BizoService,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val id: String = checkNotNull(savedStateHandle["id"])
    private val _listing = MutableStateFlow<ListingResource?>(null)
    val listing: StateFlow<ListingResource?> = _listing

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isFavorited = MutableStateFlow(false)
    val isFavorited: StateFlow<Boolean> = _isFavorited

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val currentUserId: String?
        get() = sessionManager.getUserId()

    init {
        loadListing()
        checkFavoriteStatus()
    }

    fun loadListing() {
        DebugLogger.info(LogCategory.LISTING, "Chargement de l'annonce $id")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val listing = bizoService.getListing(id).data
                _listing.value = listing
                DebugLogger.success(LogCategory.LISTING, "Annonce chargée", "Title: ${listing.title}")
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.LISTING, "Erreur chargement annonce", e.message)
                _error.value = "Impossible de charger cette annonce."
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun checkFavoriteStatus() {
        DebugLogger.info(LogCategory.FAVORITE, "Vérification statut favori pour $id")
        viewModelScope.launch {
            try {
                val response = bizoService.getFavorites()
                val isFav = response.data.any { it.listing_id == id }
                _isFavorited.value = isFav
                DebugLogger.success(LogCategory.FAVORITE, "Statut favori récupéré", "IsFavorited: $isFav")
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.FAVORITE, "Erreur vérification favori", e.message)
                e.printStackTrace()
            }
        }
    }

    fun toggleFavorite() {
        val current = _isFavorited.value
        DebugLogger.info(LogCategory.FAVORITE, "${if (current) "Retrait" else "Ajout"} du favori $id")
        viewModelScope.launch {
            try {
                if (current) bizoService.removeFavorite(id)
                else bizoService.addFavorite(id)
                _isFavorited.value = !current
                DebugLogger.success(LogCategory.FAVORITE, "Favori ${if (current) "retiré" else "ajouté"}")
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.FAVORITE, "Erreur toggle favori", e.message)
                e.printStackTrace()
            }
        }
    }

    fun deleteListing(onSuccess: () -> Unit) {
        DebugLogger.warn(LogCategory.LISTING, "Tentative de suppression de l'annonce $id")
        viewModelScope.launch {
            _isDeleting.value = true
            try {
                bizoService.deleteListing(id)
                DebugLogger.success(LogCategory.LISTING, "Annonce supprimée avec succès")
                onSuccess()
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.LISTING, "Erreur suppression annonce", e.message)
                e.printStackTrace()
            } finally {
                _isDeleting.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(navController: NavController) {
    val viewModel: ItemDetailViewModel = hiltViewModel()
    val listing by viewModel.listing.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val currentUserId = viewModel.currentUserId

    var showFullScreenPager by remember { mutableStateOf(false) }
    var selectedPhotoIndex by remember { mutableStateOf(0) }

    val isFavorited by viewModel.isFavorited.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (isLoading) {
        BizoStatePane("Chargement de l'annonce...", loading = true)
    } else if (listing == null) {
        BizoStatePane(
            text = error ?: "Annonce introuvable",
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            SecondaryButton(
                text = "Réessayer",
                onClick = { viewModel.loadListing() },
                modifier = Modifier
                    .padding(horizontal = 24.dp)
            )
        }
    } else {
        val item = listing!!
        val isOwner = item.owner?.id == currentUserId
        val photos = if (item.photos.isEmpty()) {
            listOf("https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400")
        } else {
            item.photos
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
                Surface(shadowElevation = 8.dp, tonalElevation = 4.dp) {
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
                                onClick = { 
                                    val listingId = item.id
                                    DebugLogger.info(LogCategory.NAV, "Navigation vers édition annonce", "ID: $listingId")
                                    navController.navigate("edit_listing/$listingId")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SecondaryButton(
                                text = "Supprimer",
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.weight(1f),
                                isLoading = isDeleting
                            )
                        } else {
                            PrimaryButton(
                                text = "Contacter",
                                onClick = {
                                    DebugLogger.info(LogCategory.NAV, "Navigation vers contact direct", "ListingID: ${item.id}")
                                    navController.navigate("conversation/new_${item.id}")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            SecondaryButton(
                                text = "Troc",
                                onClick = {
                                    DebugLogger.info(LogCategory.NAV, "Navigation vers proposition de troc", "ListingID: ${item.id}")
                                    navController.navigate("conversation/new_${item.id}?type=troc")
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            val pagerState = rememberPagerState(pageCount = { photos.size })
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { index ->
                            val photoUrl = photos[index]
                            AsyncImage(
                                model = MediaUrlResolver.resolve(photoUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clickable {
                                    selectedPhotoIndex = index
                                    showFullScreenPager = true
                                },
                                contentScale = ContentScale.Crop
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.type.replace("_", " "),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = item.category.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        if (photos.size > 1) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp),
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

                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = detailPriceText(item),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!item.exchange_for.isNullOrBlank()) {
                                    Text(
                                        text = "Recherche : ${item.exchange_for}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = item.condition.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = "${item.city}${if (item.neighborhood != null) " • ${item.neighborhood}" else ""}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DetailPill("Livraison", item.delivery_mode.replace("_", " "))
                            DetailPill("Vues", item.view_count.toString())
                            DetailPill("Favoris", item.favorite_count.toString())
                        }
                    }
                }

                item {
                    val owner = item.owner
                    if (owner != null) {
                        ElevatedCard(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val avatarUrl = owner.photo_url
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (avatarUrl != null) {
                                        AsyncImage(
                                            model = MediaUrlResolver.resolve(avatarUrl),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(owner.display_name.take(1))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(owner.display_name, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Note ${owner.rating ?: 0.0} • ${owner.review_count ?: 0} avis",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(onClick = { /* TODO public seller profile */ }) {
                                    Text("Profil")
                                }
                            }
                        }
                    }
                }

                item {
                    ElevatedCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Description", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(item.description, style = MaterialTheme.typography.bodyLarge)

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SpecBox(title = "Type", value = item.type)
                                SpecBox(title = "Catégorie", value = item.category)
                                SpecBox(title = "État", value = item.condition)
                                SpecBox(title = "Livraison", value = item.delivery_mode)
                                SpecBox(title = "Pays", value = item.country)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer l'annonce ?") },
            text = { Text("Cette action est irréversible. Voulez-vous vraiment supprimer cette annonce ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteListing {
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (isDeleting) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
             contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
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
                        AsyncImage(
                            model = MediaUrlResolver.resolve(photoUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    IconButton(
                        onClick = { showFullScreenPager = false },
                        modifier = Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPill(title: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = "$title · ${value.replace("_", " ")}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SpecBox(title: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.replace("_", " ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun detailPriceText(listing: ListingResource): String {
    return when (listing.type) {
        "VENTE" -> "${listing.price ?: 0} FCFA"
        "TROC_CASH" -> buildString {
            append("Troc")
            listing.cash_complement?.let {
                append(" + ")
                append(it)
                append(" FCFA")
            }
        }
        else -> "Troc"
    }
}
