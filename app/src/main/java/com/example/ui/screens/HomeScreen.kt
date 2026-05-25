package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.ListingResource
import com.example.data.api.BizoService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        viewModelScope.launch {
            try {
                val response = bizoService.getListings()
                _listings.value = response.data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, bizoService: BizoService) {
    val viewModel: HomeViewModel = remember { HomeViewModel(bizoService) }
    val listings by viewModel.listings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bizo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
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
                    navController.navigate("item_detail/${listing.id}")
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
                val fullUrl = if (photoUrl.startsWith("http")) photoUrl else "https://bizo.aiko.qzz.io$photoUrl"
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
