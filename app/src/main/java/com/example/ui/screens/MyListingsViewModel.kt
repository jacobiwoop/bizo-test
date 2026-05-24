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

class MyListingsViewModel(private val bizoService: BizoService) : ViewModel() {
    private val _listings = MutableStateFlow<List<Product>>(emptyList())
    val listings: StateFlow<List<Product>> = _listings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchMyListings()
    }

    private fun fetchMyListings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = bizoService.getMyListings()
                val list = response.data.map { listing ->
                    val id = listing.id
                    val title = listing.title
                    val price = if (!listing.price.isNullOrEmpty()) "${listing.price} FCFA" else "Gratuit / Échange"
                    val city = listing.city
                    val type = try {
                        TransactionType.valueOf(listing.type)
                    } catch (e: Exception) {
                        TransactionType.VENTE
                    }
                    
                    Product(
                        id = id,
                        title = title,
                        price = price,
                        location = city,
                        imageUrl = listing.photos.firstOrNull()?.let { "https://bizo.aiko.qzz.io$it" } ?: "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400",
                        type = type,
                        sellerName = "Moi",
                        timeAgo = "Récemment"
                    )
                }
                
                _listings.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
