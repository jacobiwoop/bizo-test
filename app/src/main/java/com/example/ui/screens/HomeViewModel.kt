package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ListingDto
import com.example.data.Product
import com.example.data.supabase
import com.example.ui.components.TransactionType
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
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
                // Fetch directy from supabase postgres
                val results = supabase.from("listings").select {
                    filter {
                        eq("status", "active")
                    }
                    // For ordering in older gotrue versions, or we can just fetch and sort locally
                    // Try to order by id
                }.decodeList<ListingDto>()
                
                val list = results.map { doc ->
                    val id = doc.id ?: ""
                    val title = doc.title
                    val priceLong = doc.price
                    val price = if (priceLong != null && priceLong > 0) "$priceLong FCFA" else "Gratuit / Échange"
                    val city = doc.city
                    val neighborhood = doc.neighborhood ?: ""
                    val location = if (neighborhood.isNotEmpty()) "$city, $neighborhood" else city
                    val type = TransactionType.values().find { it.name == doc.type } ?: TransactionType.VENTE
                    
                    Product(
                        id = id,
                        title = title,
                        price = price,
                        location = location,
                        imageUrl = "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400",
                        type = type,
                        sellerName = "Utilisateur",
                        timeAgo = "Récemment"
                    )
                }
                
                _products.value = list.reversed() // just local reverse since it's a simple app for now
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
