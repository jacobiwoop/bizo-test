package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.*
import com.example.data.api.BizoService
import com.example.ui.components.ListingItem
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyListingsViewModel(private val bizoService: BizoService) : ViewModel() {
    private val _listings = MutableStateFlow<List<ListingResource>>(emptyList())
    val listings: StateFlow<List<ListingResource>> = _listings

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadMyListings()
    }

    private fun loadMyListings() {
        DebugLogger.info(LogCategory.LISTING, "Chargement de 'Mes annonces'")
        viewModelScope.launch {
            try {
                val response = bizoService.getMyListings()
                _listings.value = response.data
                DebugLogger.success(LogCategory.LISTING, "Mes annonces chargées", "Count: ${response.data.size}")
            } catch (e: Exception) {
                DebugLogger.error(LogCategory.LISTING, "Erreur chargement mes annonces", e.message)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(navController: NavController, bizoService: BizoService) {
    val viewModel = remember { MyListingsViewModel(bizoService) }
    val listings by viewModel.listings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes annonces") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (listings.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Vous n'avez pas encore d'annonces.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(listings) { listing ->
                    ListingItem(
                        listing = listing,
                        onClick = {
                            navController.navigate("item_detail/${listing.id}")
                        }
                    )
                }
            }
        }
    }
}
