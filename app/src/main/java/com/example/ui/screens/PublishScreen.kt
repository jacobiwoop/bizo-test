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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.data.*
import com.example.data.api.BizoService
import com.example.ui.components.PrimaryButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PublishViewModel(
    private val bizoService: BizoService,
    private val listingId: String? = null
) : ViewModel() {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var price by mutableStateOf("")
    var category by mutableStateOf("AUTRES")
    var type by mutableStateOf("VENTE")
    var condition by mutableStateOf("NEUF")
    var deliveryMode by mutableStateOf("MAIN_PROPRE")
    var city by mutableStateOf("")
    var neighborhood by mutableStateOf("")
    var photos by mutableStateOf<List<String>>(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess

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
                category = item.category
                type = item.type
                condition = item.condition
                deliveryMode = item.delivery_mode
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
        val parsedPrice = price.toLongOrNull()
        
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
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class PublishViewModelFactory(
    private val bizoService: BizoService,
    private val listingId: String? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PublishViewModel(bizoService, listingId) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen(
    navController: NavController,
    bizoService: BizoService,
    listingId: String? = null
) {
    val viewModel: PublishViewModel = viewModel(factory = PublishViewModelFactory(bizoService, listingId))
    val isLoading by viewModel.isLoading.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (listingId != null) "Modifier l'annonce" else "Publier une annonce") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && listingId != null && viewModel.title.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
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
                    options = listOf("VEHICULES", "IMMOBILIER", "ELECTRONIQUE", "MAISON", "MODES", "LOISIRS", "AUTRES")
                ) { viewModel.category = it }
                
                PublishDropdown(
                    label = "Type", 
                    selected = viewModel.type, 
                    options = listOf("VENTE", "TROC", "DON")
                ) { viewModel.type = it }
                
                PublishDropdown(
                    label = "État", 
                    selected = viewModel.condition, 
                    options = listOf("NEUF", "TRES_BON_ETAT", "BON_ETAT", "SATISFAISANT")
                ) { viewModel.condition = it }
                
                PublishDropdown(
                    label = "Mode de livraison", 
                    selected = viewModel.deliveryMode, 
                    options = listOf("MAIN_PROPRE", "LIVRAISON")
                ) { viewModel.deliveryMode = it }

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = if (listingId != null) "Enregistrer les modifications" else "Publier l'annonce",
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
            value = selected.replace("_", " "),
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
                    text = { Text(option.replace("_", " ")) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
