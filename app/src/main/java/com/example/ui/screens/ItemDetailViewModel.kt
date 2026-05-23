package com.example.ui.screens

import androidx.lifecycle.ViewModel
import com.example.data.Product
import com.example.ui.components.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ItemDetailState(
    val product: Product? = null,
    val isLoading: Boolean = true,
    val isFavorite: Boolean = false,
    val error: String? = null
)

class ItemDetailViewModel : ViewModel() {
    private val db by lazy { try { FirebaseFirestore.getInstance() } catch (e: Exception) { null } }
    private val auth by lazy { try { FirebaseAuth.getInstance() } catch (e: Exception) { null } }

    private val _state = MutableStateFlow(ItemDetailState())
    val state: StateFlow<ItemDetailState> = _state.asStateFlow()

    fun loadItem(itemId: String) {
        if (db == null) {
            _state.value = ItemDetailState(isLoading = false, error = "Erreur Firebase")
            return
        }

        db?.collection("listings")?.document(itemId)?.get()?.addOnSuccessListener { doc ->
            if (doc.exists()) {
                val title = doc.getString("title") ?: ""
                val priceLong = doc.getLong("price")
                val price = if (priceLong != null && priceLong > 0) "$priceLong FCFA" else "Gratuit / Échange"
                val city = doc.getString("city") ?: ""
                val neighborhood = doc.getString("neighborhood") ?: ""
                val location = if (neighborhood.isNotEmpty()) "$city, $neighborhood" else city
                val typeStr = doc.getString("type")
                val type = TransactionType.values().find { it.name == typeStr } ?: TransactionType.VENTE
                
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
        }?.addOnFailureListener { e ->
            _state.value = ItemDetailState(isLoading = false, error = e.message)
        }
    }

    private fun checkFavorite(itemId: String) {
        val uid = auth?.currentUser?.uid ?: return
        db?.collection("favorites")?.document(uid)?.collection("items")?.document(itemId)?.get()?.addOnSuccessListener { doc ->
            if (doc.exists()) {
                _state.value = _state.value.copy(isFavorite = true)
            }
        }
    }

    fun toggleFavorite(itemId: String) {
        val uid = auth?.currentUser?.uid ?: return
        if (db == null) return

        val ref = db!!.collection("favorites").document(uid).collection("items").document(itemId)
        val currentFav = _state.value.isFavorite
        
        // Optimistic UI update
        _state.value = _state.value.copy(isFavorite = !currentFav)

        if (currentFav) {
            ref.delete()
        } else {
            val p = _state.value.product ?: return
            val favData = hashMapOf(
                "listingId" to p.id,
                "listingTitle" to p.title,
                "listingPhoto" to p.imageUrl,
                "type" to p.type.name,
                "addedAt" to com.google.firebase.Timestamp.now()
            )
            ref.set(favData)
        }
    }
}
