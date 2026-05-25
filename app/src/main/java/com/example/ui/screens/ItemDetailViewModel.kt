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

data class ItemDetailState(
    val product: Product? = null,
    val isLoading: Boolean = true,
    val isFavorite: Boolean = false,
    val error: String? = null,
    val conversationId: String? = null
)

class ItemDetailViewModel(private val bizoService: BizoService) : ViewModel() {
    private val _state = MutableStateFlow(ItemDetailState())
    val state: StateFlow<ItemDetailState> = _state.asStateFlow()

    fun contactSeller(onSuccess: (String) -> Unit) {
        val product = _state.value.product ?: return
        viewModelScope.launch {
            try {
                val response = bizoService.createConversation(product.id, "Bonjour, je suis intéressé par votre annonce ${product.title}.")
                _state.value = _state.value.copy(conversationId = response.data.id)
                onSuccess(response.data.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            try {
                val response = bizoService.getListingDetails(itemId)
                val listing = response.data
                
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
                
                val product = Product(
                    id = itemId,
                    title = title,
                    price = price,
                    location = location,
                    imageUrl = listing.photos.firstOrNull()?.let { path ->
                        if (path.startsWith("http")) path else "https://bizo.aiko.qzz.io$path"
                    } ?: "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400",
                    type = type,
                    sellerName = listing.owner?.display_name ?: "Utilisateur",
                    timeAgo = "Récemment",
                    description = listing.description,
                    category = listing.category,
                    condition = listing.condition,
                    deliveryMode = listing.delivery_mode,
                    photos = listing.photos.map { path ->
                        if (path.startsWith("http")) path else "https://bizo.aiko.qzz.io$path"
                    }
                )
                
                _state.value = _state.value.copy(product = product, isLoading = false)
                checkFavorite(itemId)
            } catch (e: Exception) {
                _state.value = ItemDetailState(isLoading = false, error = e.message)
            }
        }
    }

    private fun checkFavorite(itemId: String) {
        viewModelScope.launch {
            try {
                val favorites = bizoService.getFavorites()
                val isFavorite = favorites.data.any { it.listing_id == itemId }
                _state.value = _state.value.copy(isFavorite = isFavorite)
            } catch (e: Exception) {
                // Ignore if not logged in or error
            }
        }
    }

    fun toggleFavorite(itemId: String) {
        val currentFav = _state.value.isFavorite
        _state.value = _state.value.copy(isFavorite = !currentFav)

        viewModelScope.launch {
            try {
                bizoService.toggleFavorite(itemId, currentFav)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isFavorite = currentFav)
            }
        }
    }
}
