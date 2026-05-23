package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FavoriteDto
import com.example.data.ListingDto
import com.example.data.Product
import com.example.data.supabase
import com.example.ui.components.TransactionType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class ItemDetailState(
    val product: Product? = null,
    val isLoading: Boolean = true,
    val isFavorite: Boolean = false,
    val error: String? = null
)

class ItemDetailViewModel : ViewModel() {
    private val _state = MutableStateFlow(ItemDetailState())
    val state: StateFlow<ItemDetailState> = _state.asStateFlow()

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            try {
                val doc = supabase.from("listings").select {
                    filter {
                        eq("id", itemId)
                    }
                }.decodeSingleOrNull<ListingDto>()
                
                if (doc != null) {
                    val title = doc.title
                    val priceLong = doc.price
                    val price = if (priceLong != null && priceLong > 0) "$priceLong FCFA" else "Gratuit / Échange"
                    val city = doc.city
                    val neighborhood = doc.neighborhood ?: ""
                    val location = if (neighborhood.isNotEmpty()) "$city, $neighborhood" else city
                    val type = TransactionType.values().find { it.name == doc.type } ?: TransactionType.VENTE
                    
                    val product = Product(
                        id = itemId,
                        title = title,
                        price = price,
                        location = location,
                        imageUrl = "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400",
                        type = type,
                        sellerName = "Utilisateur",
                        timeAgo = "Récemment" // TODO real time
                    )
                    
                    _state.value = _state.value.copy(product = product, isLoading = false)
                    checkFavorite(itemId)
                } else {
                    _state.value = ItemDetailState(isLoading = false, error = "Annonce introuvable")
                }
            } catch (e: Exception) {
                _state.value = ItemDetailState(isLoading = false, error = e.message)
            }
        }
    }

    private fun checkFavorite(itemId: String) {
        viewModelScope.launch {
            val uid = try { supabase.auth.currentSessionOrNull()?.user?.id } catch (e: Exception) { null } ?: return@launch
            try {
                val favs = supabase.from("favorites").select {
                    filter {
                        eq("listingId", itemId)
                        eq("uid", uid)
                    }
                }.decodeList<FavoriteDto>()
                
                if (favs.isNotEmpty()) {
                    _state.value = _state.value.copy(isFavorite = true)
                }
            } catch (e: Exception) {}
        }
    }

    fun toggleFavorite(itemId: String) {
        val currentFav = _state.value.isFavorite
        _state.value = _state.value.copy(isFavorite = !currentFav)

        viewModelScope.launch {
            val uid = try { supabase.auth.currentSessionOrNull()?.user?.id } catch (e: Exception) { null }
            if (uid == null) {
                _state.value = _state.value.copy(isFavorite = currentFav)
                return@launch
            }

            try {
                if (currentFav) {
                    supabase.from("favorites").delete {
                        filter {
                            eq("listingId", itemId)
                            eq("uid", uid)
                        }
                    }
                } else {
                    val favData = FavoriteDto(
                        listingId = itemId,
                        uid = uid,
                        addedAt = Date().toString()
                    )
                    supabase.from("favorites").insert(favData)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isFavorite = currentFav)
            }
        }
    }
}
