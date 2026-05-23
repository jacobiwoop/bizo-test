package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.ui.components.TransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class PublishViewModel : ViewModel() {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var condition by mutableStateOf("")
    var deliveryMode by mutableStateOf("")
    var category by mutableStateOf("")
    var type by mutableStateOf<TransactionType?>(null)
    var price by mutableStateOf("")
    var exchangeFor by mutableStateOf("")
    var cashComplement by mutableStateOf("")
    
    var country by mutableStateOf("Bénin")
    var city by mutableStateOf("Cotonou")
    var neighborhood by mutableStateOf("")

    var currentStep by mutableStateOf(1)
    var isPublishing by mutableStateOf(false)
    var publishSuccess by mutableStateOf(false)

    fun publish() {
        val uid = try { FirebaseAuth.getInstance().currentUser?.uid } catch (e: Exception) { null } ?: return
        isPublishing = true
        
        val listingId = UUID.randomUUID().toString()
        val db = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
        
        val docData = hashMapOf(
            "id" to listingId,
            "ownerUid" to uid,
            "title" to title,
            "description" to description,
            "condition" to condition,
            "deliveryMode" to deliveryMode,
            "category" to category,
            "type" to type?.name,
            "price" to price.toLongOrNull(),
            "exchangeFor" to exchangeFor,
            "cashComplement" to cashComplement.toLongOrNull(),
            "country" to country,
            "city" to city,
            "neighborhood" to neighborhood,
            "status" to "active",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "viewCount" to 0,
            "favoriteCount" to 0
        )

        db?.collection("listings")?.document(listingId)?.set(docData)
            ?.addOnSuccessListener {
                isPublishing = false
                publishSuccess = true
            }
            ?.addOnFailureListener {
                isPublishing = false
            }
    }
}
