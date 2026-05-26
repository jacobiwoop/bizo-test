package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.data.*
import com.example.ui.components.BizoScreen
import com.example.ui.components.BizoStatePane
import com.example.ui.components.PrimaryButton
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream

@HiltViewModel
class PublishViewModel @Inject constructor(
    private val bizoService: com.example.data.api.BizoService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val listingId: String? = savedStateHandle["id"]
    private val json = Json { ignoreUnknownKeys = true }
    val isEditMode: Boolean
        get() = listingId != null
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var price by mutableStateOf("")
    var category by mutableStateOf("electronique")
    var type by mutableStateOf("VENTE")
    var condition by mutableStateOf("neuf")
    var deliveryMode by mutableStateOf("main_propre")
    var city by mutableStateOf("")
    var neighborhood by mutableStateOf("")
    var existingPhotos by mutableStateOf<List<String>>(emptyList())
    var selectedPhotoUris by mutableStateOf<List<String>>(emptyList())
    var exchangeFor by mutableStateOf("")
    var cashComplement by mutableStateOf("")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        if (listingId != null) {
            loadListingForEdit(listingId)
        }
    }

    private fun loadListingForEdit(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val item = bizoService.getListing(id)
                title = item.title
                description = item.description
                price = item.price?.toString() ?: ""
                category = item.category.lowercase()
                type = item.type
                condition = item.condition.lowercase()
                deliveryMode = item.delivery_mode.lowercase()
                city = item.city
                neighborhood = item.neighborhood ?: ""
                existingPhotos = item.photos
                exchangeFor = item.exchange_for ?: ""
                cashComplement = item.cash_complement?.toString() ?: ""
                DebugLogger.success(LogCategory.LISTING, "Annonce chargée pour édition", "ID: $id")
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.LISTING, "Erreur chargement annonce pour édition", e.message)
                _error.value = "Impossible de charger l'annonce."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addPhoto(uri: Uri) {
        selectedPhotoUris = (selectedPhotoUris + uri.toString()).distinct()
    }

    fun removePhoto(uriString: String) {
        selectedPhotoUris = selectedPhotoUris.filterNot { it == uriString }
    }

    private fun mapValidationError(exception: HttpException): String {
        val errorBody = exception.response()?.errorBody()?.string().orEmpty()
        return runCatching {
            val payload = json.decodeFromString<ValidationErrorResponse>(errorBody)
            payload.errors.values.firstOrNull()?.firstOrNull()
                ?: payload.message
                ?: "Erreur de validation."
        }.getOrElse {
            "Erreur de validation."
        }
    }

    private fun buildFieldMap(): Map<String, okhttp3.RequestBody> {
        val fields = linkedMapOf<String, okhttp3.RequestBody>()
        fun put(name: String, value: String?) {
            if (!value.isNullOrBlank()) {
                fields[name] = value.toRequestBody("text/plain".toMediaType())
            }
        }

        put("title", title)
        put("description", description)
        put("category", category)
        put("type", type)
        put("condition", condition)
        put("delivery_mode", deliveryMode)
        put("country", "BJ")
        put("city", city)
        put("neighborhood", neighborhood)
        put("exchange_for", exchangeFor.takeIf { it.isNotBlank() })
        put("cash_complement", cashComplement.takeIf { it.isNotBlank() })
        put("price", price.takeIf { it.isNotBlank() })

        return fields
    }

    private fun uriToMultipart(context: Context, uriString: String, index: Int): MultipartBody.Part {
        val uri = Uri.parse(uriString)
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "image/*"
        val extension = mimeType.substringAfter('/', "jpg")
        val tempFile = File(context.cacheDir, "listing_photo_${System.currentTimeMillis()}_$index.$extension")
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Impossible de lire la photo sélectionnée." }
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }
        val requestBody = tempFile.asRequestBody(mimeType.toMediaType())
        return MultipartBody.Part.createFormData("photos[$index]", tempFile.name, requestBody)
    }

    fun submit(context: Context, onSuccess: () -> Unit) {
        if (title.isBlank() || description.isBlank() || city.isBlank()) {
            _error.value = "Titre, description et ville sont obligatoires."
            return
        }

        val parsedPrice = price.toLongOrNull()
        if (price.isNotBlank() && parsedPrice == null) {
            _error.value = "Le prix doit etre numerique."
            return
        }

        val parsedCashComplement = cashComplement.toLongOrNull()
        if (cashComplement.isNotBlank() && parsedCashComplement == null) {
            _error.value = "Le complément cash doit etre numerique."
            return
        }

        if (type == "VENTE" && parsedPrice == null) {
            _error.value = "Le prix est requis pour une vente."
            return
        }

        if ((type == "TROC" || type == "TROC_CASH") && exchangeFor.isBlank()) {
            _error.value = "Precisez ce que vous cherchez en échange."
            return
        }

        if (!isEditMode && selectedPhotoUris.isEmpty()) {
            _error.value = "Au moins une photo est requise pour publier une annonce."
            return
        }

        val request = ListingRequest(
            title = title,
            description = description,
            category = category,
            type = type,
            condition = condition,
            delivery_mode = deliveryMode,
            price = parsedPrice,
            cash_complement = parsedCashComplement,
            exchange_for = exchangeFor.takeIf { it.isNotBlank() },
            city = city,
            neighborhood = neighborhood,
            country = "BJ"
        )

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                if (listingId != null) {
                    DebugLogger.info(LogCategory.LISTING, "Mise à jour de l'annonce $listingId")
                    bizoService.updateListing(listingId, request)
                    DebugLogger.success(LogCategory.LISTING, "Annonce mise à jour avec succès")
                } else {
                    DebugLogger.info(LogCategory.LISTING, "Création d'une nouvelle annonce")
                    val parts = selectedPhotoUris.mapIndexed { index, uriString ->
                        uriToMultipart(context, uriString, index)
                    }
                    bizoService.createListing(buildFieldMap(), parts)
                    DebugLogger.success(LogCategory.LISTING, "Annonce créée avec succès")
                }
                _isSuccess.value = true
                onSuccess()
            } catch (e: HttpException) {
                DebugLogger.error(LogCategory.LISTING, "Erreur validation annonce", e.message())
                _error.value = mapValidationError(e)
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.LISTING, "Erreur lors de la soumission de l'annonce", e.message)
                _error.value = "Impossible d'enregistrer l'annonce."
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(navController: NavController) {
    val viewModel: PublishViewModel = hiltViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val isEditMode = viewModel.isEditMode
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        uris.forEach(viewModel::addPhoto)
    }

    BizoScreen(
        title = if (isEditMode) "Modifier l'annonce" else "Publier une annonce",
        onBack = { navController.popBackStack() }
    ) { padding ->
        if (isLoading && isEditMode && viewModel.title.isEmpty()) {
            BizoStatePane("Chargement de l'annonce...", modifier = Modifier.padding(padding), loading = true)
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.title,
                    onValueChange = { viewModel.title = it },
                    label = { Text("Titre") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.description,
                    onValueChange = { viewModel.description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = viewModel.price,
                        onValueChange = { viewModel.price = it },
                        label = { Text("Prix (FCFA)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.city,
                        onValueChange = { viewModel.city = it },
                        label = { Text("Ville") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = viewModel.neighborhood,
                    onValueChange = { viewModel.neighborhood = it },
                    label = { Text("Quartier") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (viewModel.type == "TROC" || viewModel.type == "TROC_CASH") {
                    OutlinedTextField(
                        value = viewModel.exchangeFor,
                        onValueChange = { viewModel.exchangeFor = it },
                        label = { Text("Recherche en échange") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (viewModel.type == "TROC_CASH") {
                    OutlinedTextField(
                        value = viewModel.cashComplement,
                        onValueChange = { viewModel.cashComplement = it },
                        label = { Text("Complément cash") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Détails supplémentaires", style = MaterialTheme.typography.titleMedium)
                
                PublishDropdown(
                    label = "Catégorie", 
                    selected = viewModel.category, 
                    options = listOf("electronique", "vetements", "vehicules", "maison", "services")
                ) { viewModel.category = it }
                
                PublishDropdown(
                    label = "Type", 
                    selected = viewModel.type, 
                    options = listOf("VENTE", "TROC", "TROC_CASH")
                ) { viewModel.type = it }
                
                PublishDropdown(
                    label = "État", 
                    selected = viewModel.condition, 
                    options = listOf("neuf", "excellent", "bon", "correct")
                ) { viewModel.condition = it }
                
                PublishDropdown(
                    label = "Mode de livraison", 
                    selected = viewModel.deliveryMode, 
                    options = listOf("main_propre", "livraison", "les_deux")
                ) { viewModel.deliveryMode = it }

                Text("Photos", style = MaterialTheme.typography.titleMedium)

                if (!isEditMode) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ajouter des photos")
                    }
                    if (viewModel.selectedPhotoUris.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            viewModel.selectedPhotoUris.forEach { uriString ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = Uri.parse(uriString),
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Photo sélectionnée",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(onClick = { viewModel.removePhoto(uriString) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Note: L'édition des photos n'est pas encore disponible. Les photos existantes seront conservées.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (viewModel.existingPhotos.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            viewModel.existingPhotos.forEach { photoUrl ->
                                AsyncImage(
                                    model = MediaUrlResolver.resolve(photoUrl),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = if (isEditMode) "Enregistrer les modifications" else "Publier l'annonce",
                    onClick = {
                        viewModel.submit(context) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoading
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishDropdown(label: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.replace("_", " ").uppercase(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replace("_", " ").uppercase()) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
