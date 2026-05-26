package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.*
import com.example.data.api.BizoService
import com.example.ui.components.BizoListingCard
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
class MyListingsViewModel @Inject constructor(private val bizoService: BizoService) : ViewModel() {
    private val _listings = MutableStateFlow<List<ListingResource>>(emptyList())
    val listings: StateFlow<List<ListingResource>> = _listings

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadMyListings()
    }

    private fun loadMyListings() {
        DebugLogger.info(LogCategory.LISTING, "Chargement de 'Mes annonces'")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = bizoService.getMyListings()
                _listings.value = response.data
                DebugLogger.success(LogCategory.LISTING, "Mes annonces chargées", "Count: ${response.data.size}")
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.LISTING, "Erreur chargement mes annonces", e.message)
                _error.value = "Impossible de charger vos annonces."
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@Composable
fun MyListingsScreen(navController: NavController, bizoService: BizoService) {
    val viewModel: MyListingsViewModel = hiltViewModel()
    val listings by viewModel.listings.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    BizoScreen(
        title = "Mes annonces",
        subtitle = "Gérez vos publications actives",
        onBack = { navController.popBackStack() }
    ) { padding ->
        if (isLoading) {
            BizoStatePane("Chargement...", modifier = Modifier.padding(padding), loading = true)
        } else if (error != null && listings.isEmpty()) {
            BizoStatePane(error!!, modifier = Modifier.padding(padding))
        } else if (listings.isEmpty()) {
            BizoStatePane("Vous n'avez pas encore d'annonces.", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(listings) { listing ->
                    BizoListingCard(
                        listing = listing,
                        showOwnerBadge = false,
                        onClick = { navController.navigate("item_detail/${listing.id}") }
                    )
                }
            }
        }
    }
}
