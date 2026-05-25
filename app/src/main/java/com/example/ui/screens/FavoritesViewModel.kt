package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BizoService
import com.example.data.Product
import com.example.ui.components.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(private val bizoService: BizoService) : ViewModel() {
    private val _favorites = MutableStateFlow<List<Product>>(emptyList())
    val favorites: StateFlow<List<Product>> = _favorites.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchFavorites()
    }

    private fun fetchFavorites() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = bizoService.getFavorites()
                val list = response.data.mapNotNull { fav ->
                    val listing = fav.listing ?: return@mapNotNull null
                    val id = listing.id
                    val title = listing.title
                    val price = listing.price?.let { "$it FCFA" } ?: "Gratuit / Échange"
                    val city = listing.city
                    val neighborhood = listing.neighborhood ?: ""
                    val location = if (neighborhood.isNotEmpty()) "$city, $neighborhood" else city
                    val type = try {
                        TransactionType.valueOf(listing.type)
                    } catch (e: Exception) {
                        TransactionType.VENTE
                    }
                    
                    Product(
                        id = id,
                        title = title,
                        price = price,
                        location = location,
                        imageUrl = listing.photos.firstOrNull()?.let { path ->
                            if (path.startsWith("http")) path else "https://bizo.aiko.qzz.io$path"
                        } ?: "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400",
                        type = type,
                        sellerName = listing.owner?.display_name ?: "Utilisateur",
                        timeAgo = "Favori"
                    )
                }
                
                _favorites.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
