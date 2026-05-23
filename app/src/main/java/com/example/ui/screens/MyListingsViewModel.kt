package com.example.ui.screens

import androidx.lifecycle.ViewModel
import com.example.data.Product
import com.example.ui.components.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MyListingsViewModel : ViewModel() {
    private val db by lazy { try { FirebaseFirestore.getInstance() } catch (e: Exception) { null } }
    private val auth by lazy { try { FirebaseAuth.getInstance() } catch (e: Exception) { null } }

    private val _listings = MutableStateFlow<List<Product>>(emptyList())
    val listings: StateFlow<List<Product>> = _listings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchMyListings()
    }

    private fun fetchMyListings() {
        val uid = auth?.currentUser?.uid
        if (uid == null || db == null) {
            _isLoading.value = false
            return
        }

        db?.collection("listings")
            ?.whereEqualTo("ownerUid", uid)
            ?.orderBy("createdAt", Query.Direction.DESCENDING)
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
                    val typeStr = doc.getString("type")
                    val type = TransactionType.values().find { it.name == typeStr } ?: TransactionType.VENTE
                    
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

                _listings.value = list
                _isLoading.value = false
            }
    }
}
