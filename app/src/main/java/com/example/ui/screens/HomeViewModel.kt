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

class HomeViewModel(private val bizoService: BizoService) : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchProducts()
    }

    fun fetchProducts() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = bizoService.getListings()
                val list = response.data.map { listing ->
                    val id = listing.id
                    val title = listing.title
                    val price = if (!listing.price.isNullOrEmpty()) "${listing.price} FCFA" else "Gratuit / Échange"
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
                        imageUrl = listing.photos.firstOrNull()?.let { "https://bizo.aiko.qzz.io$it" } ?: "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400",
                        type = type,
                        sellerName = listing.owner?.display_name ?: "Utilisateur",
                        timeAgo = "Récemment"
                    )
                }
                
                _products.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
