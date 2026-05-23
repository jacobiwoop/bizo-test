package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class MyListingsViewModel : ViewModel() {
    private val _listings = MutableStateFlow<List<Product>>(emptyList())
    val listings: StateFlow<List<Product>> = _listings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchMyListings()
    }

    private fun fetchMyListings() {
        viewModelScope.launch {
            val uid = try { supabase.auth.currentSessionOrNull()?.user?.id } catch (e: Exception) { null }
            if (uid == null) {
                _isLoading.value = false
                return@launch
            }

            try {
                val results = supabase.from("listings").select {
                    filter {
                        eq("ownerUid", uid)
                    }
                }.decodeList<ListingDto>()
                
                val list = results.map { doc ->
                    val id = doc.id ?: ""
                    val title = doc.title
                    val priceLong = doc.price
                    val price = if (priceLong != null && priceLong > 0) "$priceLong FCFA" else "Gratuit / Échange"
                    val city = doc.city
                    val type = TransactionType.values().find { it.name == doc.type } ?: TransactionType.VENTE
                    
                    Product(
                        id = id,
                        title = title,
                        price = price,
                        location = city,
                        imageUrl = "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400", // placeholder
                        type = type,
                        sellerName = "Moi",
                        timeAgo = ""
                    )
                }
                
                _listings.value = list.reversed()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
