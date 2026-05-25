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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        fetchProducts()
    }

    fun fetchProducts() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val response = bizoService.getListings()
                val list = response.data.map { listing ->
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
                    
                    val photoUrl = listing.photos.firstOrNull()?.let { path ->
                        if (path.startsWith("http")) {
                            path
                        } else {
                            "https://bizo.aiko.qzz.io$path"
                        }
                    } ?: "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400"

                    Product(
                        id = id,
                        title = title,
                        price = price,
                        location = location,
                        imageUrl = photoUrl,
                        type = type,
                        sellerName = listing.owner?.display_name ?: "Utilisateur",
                        timeAgo = "Récemment"
                    )
                }
                
                _products.value = list
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = e.message ?: "Une erreur est survenue lors du chargement des annonces."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
