package com.example.ui.screens

import androidx.lifecycle.ViewModel
import com.example.data.Product
import com.example.ui.components.TransactionType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {
    private val db by lazy { try { FirebaseFirestore.getInstance() } catch (e: Exception) { null } }

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchProducts()
    }

    fun fetchProducts() {
        if (db == null) {
            _isLoading.value = false
            return
        }
        _isLoading.value = true
        db?.collection("listings")
            ?.whereEqualTo("status", "active")
            ?.orderBy("createdAt", Query.Direction.DESCENDING)
            ?.limit(20)
            ?.addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    val priceLong = doc.getLong("price")
                    val price = if (priceLong != null && priceLong > 0) "$priceLong FCFA" else "Gratuit / Échange"
                    val city = doc.getString("city") ?: ""
                    val neighborhood = doc.getString("neighborhood") ?: ""
                    val location = if (neighborhood.isNotEmpty()) "$city, $neighborhood" else city
                    val typeStr = doc.getString("type")
                    val type = TransactionType.values().find { it.name == typeStr } ?: TransactionType.VENTE
                    
                    // On mocke un peu ce qu'on n'a pas encore stocké correctement de l'owner
                    Product(
                        id = id,
                        title = title,
                        price = price,
                        location = location,
                        imageUrl = "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400", // placeholder as image upload is mock
                        type = type,
                        sellerName = "Utilisateur",
                        timeAgo = "Récemment"
                    )
                }

                _products.value = list
                _isLoading.value = false
            }
    }
}
