package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.components.SecondaryButton
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
                val item = bizoService.getListing(id).data
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

    fun clearError() {
        _error.value = null
    }

    fun validateStep(step: Int): Boolean {
        _error.value = when (step) {
            0 -> {
                if (!isEditMode && selectedPhotoUris.isEmpty()) {
                    "Ajoutez au moins une photo pour continuer."
                } else {
                    null
                }
            }
            1 -> {
                val parsedPrice = price.toLongOrNull()
                val parsedCashComplement = cashComplement.toLongOrNull()
                when {
                    price.isNotBlank() && parsedPrice == null -> "Le prix doit etre numerique."
                    cashComplement.isNotBlank() && parsedCashComplement == null -> "Le complément cash doit etre numerique."
                    type == "VENTE" && parsedPrice == null -> "Le prix est requis pour une vente."
                    (type == "TROC" || type == "TROC_CASH") && exchangeFor.isBlank() ->
                        "Precisez ce que vous cherchez en échange."
                    else -> null
                }
            }
            else -> {
                when {
                    title.isBlank() -> "Le titre est obligatoire."
                    description.isBlank() -> "La description est obligatoire."
                    city.isBlank() -> "La ville est obligatoire."
                    else -> null
                }
            }
        }

        return _error.value == null
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
        if (type == "VENTE") {
            put("price", price.takeIf { it.isNotBlank() })
        }
        if (type == "TROC" || type == "TROC_CASH") {
            put("exchange_for", exchangeFor.takeIf { it.isNotBlank() })
        }
        if (type == "TROC_CASH") {
            put("cash_complement", cashComplement.takeIf { it.isNotBlank() })
        }

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
        return MultipartBody.Part.createFormData("photos[]", tempFile.name, requestBody)
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
            price = parsedPrice.takeIf { type == "VENTE" },
            cash_complement = parsedCashComplement.takeIf { type == "TROC_CASH" },
            exchange_for = exchangeFor.takeIf { (type == "TROC" || type == "TROC_CASH") && it.isNotBlank() },
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PublishScreen(navController: NavController) {
    val viewModel: PublishViewModel = hiltViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val isEditMode = viewModel.isEditMode
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = listOf("Photos", "Transaction", "Détails")
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
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEditMode) "Affinez votre annonce" else "Créez une annonce qui donne envie",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEditMode)
                                "Mettez à jour le contenu, le type de transaction et la localisation. Les photos existantes restent conservées."
                            else
                                "Commencez par les visuels, puis décrivez clairement l'objet et la transaction attendue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                PublishStepHeader(
                    currentStep = currentStep,
                    steps = steps
                )

                when (currentStep) {
                    0 -> PublishSectionCard(
                        title = "Photos",
                        subtitle = if (isEditMode) "Les photos actuelles sont affichées ci-dessous." else "Ajoutez entre 1 et 10 images nettes."
                    ) {
                        if (!isEditMode) {
                            OutlinedButton(
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Choisir des photos")
                            }
                        } else {
                            Text(
                                text = "L'édition des photos sera gérée dans une passe dédiée. Pour l'instant, elles sont conservées.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val visualItems = if (isEditMode) viewModel.existingPhotos else viewModel.selectedPhotoUris
                        if (visualItems.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                visualItems.forEach { value ->
                                    Surface(
                                        modifier = Modifier.width(104.dp),
                                        shape = MaterialTheme.shapes.medium,
                                        tonalElevation = 2.dp
                                    ) {
                                        Column {
                                            AsyncImage(
                                                model = if (isEditMode) MediaUrlResolver.resolve(value) else Uri.parse(value),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(96.dp)
                                                    .clip(MaterialTheme.shapes.medium),
                                                contentScale = ContentScale.Crop
                                            )
                                            if (!isEditMode) {
                                                TextButton(
                                                    onClick = { viewModel.removePhoto(value) },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Retirer")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> PublishSectionCard(
                        title = "Transaction",
                        subtitle = "Choisissez la logique métier exacte de l'annonce."
                    ) {
                        Text(
                            text = "Type de transaction",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("VENTE", "TROC", "TROC_CASH").forEach { option ->
                                FilterChip(
                                    selected = viewModel.type == option,
                                    onClick = {
                                        viewModel.type = option
                                        viewModel.clearError()
                                    },
                                    label = { Text(option.replace("_", " ")) }
                                )
                            }
                        }

                        if (viewModel.type == "VENTE") {
                            OutlinedTextField(
                                value = viewModel.price,
                                onValueChange = { viewModel.price = it },
                                label = { Text("Prix (FCFA)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (viewModel.type == "TROC" || viewModel.type == "TROC_CASH") {
                            OutlinedTextField(
                                value = viewModel.exchangeFor,
                                onValueChange = { viewModel.exchangeFor = it },
                                label = { Text("Je cherche en échange") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (viewModel.type == "TROC_CASH") {
                            OutlinedTextField(
                                value = viewModel.cashComplement,
                                onValueChange = { viewModel.cashComplement = it },
                                label = { Text("Complément cash") },
                                supportingText = { Text("Optionnel selon l'accord souhaité") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    else -> {
                        PublishSectionCard(
                            title = "Présentation",
                            subtitle = "Donnez rapidement envie de cliquer et de lire."
                        ) {
                            OutlinedTextField(
                                value = viewModel.title,
                                onValueChange = { viewModel.title = it },
                                label = { Text("Titre") },
                                supportingText = { Text("5 à 80 caractères") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = viewModel.description,
                                onValueChange = { viewModel.description = it },
                                label = { Text("Description") },
                                supportingText = { Text("20 à 500 caractères") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4
                            )
                        }

                        PublishSectionCard(
                            title = "Classification",
                            subtitle = "Ces informations aident le tri, la recherche et les filtres."
                        ) {
                            PublishDropdown(
                                label = "Catégorie",
                                selected = viewModel.category,
                                options = listOf("electronique", "vetements", "vehicules", "maison", "services")
                            ) { viewModel.category = it }

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
                        }

                        PublishSectionCard(
                            title = "Localisation",
                            subtitle = "Aidez l'acheteur à savoir où la transaction peut se faire."
                        ) {
                            OutlinedTextField(
                                value = viewModel.city,
                                onValueChange = { viewModel.city = it },
                                label = { Text("Ville") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = viewModel.neighborhood,
                                onValueChange = { viewModel.neighborhood = it },
                                label = { Text("Quartier") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        PublishSectionCard(
                            title = "Résumé",
                            subtitle = "Vérifiez l'essentiel avant publication."
                        ) {
                            DetailSummaryRow("Photos", if (isEditMode) "${viewModel.existingPhotos.size}" else "${viewModel.selectedPhotoUris.size}")
                            DetailSummaryRow("Type", viewModel.type.replace("_", " "))
                            DetailSummaryRow(
                                "Valeur",
                                when (viewModel.type) {
                                    "VENTE" -> if (viewModel.price.isBlank()) "Prix à renseigner" else "${viewModel.price} FCFA"
                                    "TROC_CASH" -> buildString {
                                        append(viewModel.exchangeFor.ifBlank { "Échange à préciser" })
                                        if (viewModel.cashComplement.isNotBlank()) {
                                            append(" + ")
                                            append(viewModel.cashComplement)
                                            append(" FCFA")
                                        }
                                    }
                                    else -> viewModel.exchangeFor.ifBlank { "Échange à préciser" }
                                }
                            )
                            DetailSummaryRow("Ville", viewModel.city.ifBlank { "À renseigner" })
                        }
                    }
                }

                if (error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 0) {
                        SecondaryButton(
                            text = "Retour",
                            onClick = {
                                viewModel.clearError()
                                currentStep -= 1
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PrimaryButton(
                        text = when {
                            currentStep < steps.lastIndex -> "Continuer"
                            isEditMode -> "Enregistrer les modifications"
                            else -> "Publier l'annonce"
                        },
                        onClick = {
                            if (currentStep < steps.lastIndex) {
                                if (viewModel.validateStep(currentStep)) {
                                    currentStep += 1
                                }
                            } else {
                                viewModel.submit(context) {
                                    navController.popBackStack()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        isLoading = isLoading
                    )
                }
            }
        }
    }
}

@Composable
private fun PublishStepHeader(
    currentStep: Int,
    steps: List<String>
) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Étape ${currentStep + 1} sur ${steps.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                steps.forEachIndexed { index, label ->
                    val active = index == currentStep
                    val completed = index < currentStep
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        color = when {
                            active -> MaterialTheme.colorScheme.primary
                            completed -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (completed) "OK" else "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                content()
            }
        )
    }
}

@Composable
private fun DetailSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
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
