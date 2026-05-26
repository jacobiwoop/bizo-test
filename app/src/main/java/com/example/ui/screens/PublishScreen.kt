package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PublishViewModel @Inject constructor(
    private val bizoService: com.example.data.api.BizoService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val listingId: String? = savedStateHandle["id"]
    val isEditMode: Boolean
        get() = listingId != null
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var price by mutableStateOf("")
    var category by mutableStateOf("autres")
    var type by mutableStateOf("vente")
    var condition by mutableStateOf("neuf")
    var deliveryMode by mutableStateOf("main_propre")
    var city by mutableStateOf("")
    var neighborhood by mutableStateOf("")
    var photos by mutableStateOf<List<String>>(emptyList())

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
                val response = bizoService.getListing(id)
                val item = response.data
                title = item.title
                description = item.description
                price = item.price?.toString() ?: ""
                category = item.category.lowercase()
                type = item.type.lowercase()
                condition = item.condition.lowercase()
                deliveryMode = item.delivery_mode.lowercase()
                city = item.city
                neighborhood = item.neighborhood ?: ""
                photos = item.photos
                DebugLogger.success(LogCategory.LISTING, "Annonce chargée pour édition", "ID: $id")
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.LISTING, "Erreur chargement annonce pour édition", e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submit(onSuccess: () -> Unit) {
        if (title.isBlank() || description.isBlank() || city.isBlank()) {
            _error.value = "Titre, description et ville sont obligatoires."
            return
        }

        val parsedPrice = price.toLongOrNull()
        if (price.isNotBlank() && parsedPrice == null) {
            _error.value = "Le prix doit etre numerique."
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
            photos = photos,
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
                    bizoService.createListing(request)
                    DebugLogger.success(LogCategory.LISTING, "Annonce créée avec succès")
                }
                _isSuccess.value = true
                onSuccess()
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
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val isEditMode = viewModel.isEditMode

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

                Text("Détails supplémentaires", style = MaterialTheme.typography.titleMedium)
                
                PublishDropdown(
                    label = "Catégorie", 
                    selected = viewModel.category, 
                    options = listOf("electronique", "vetements", "vehicules", "maison", "services", "autres")
                ) { viewModel.category = it }
                
                PublishDropdown(
                    label = "Type", 
                    selected = viewModel.type, 
                    options = listOf("vente", "troc", "don")
                ) { viewModel.type = it }
                
                PublishDropdown(
                    label = "État", 
                    selected = viewModel.condition, 
                    options = listOf("neuf", "tres_bon_etat", "bon_etat", "satisfaisant")
                ) { viewModel.condition = it }
                
                PublishDropdown(
                    label = "Mode de livraison", 
                    selected = viewModel.deliveryMode, 
                    options = listOf("main_propre", "livraison")
                ) { viewModel.deliveryMode = it }

                if (isEditMode) {
                    Text(
                        text = "Note: L'édition des photos n'est pas encore disponible. Les photos existantes seront conservées.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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
                        viewModel.submit {
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
