package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ListingDto
import com.example.data.supabase
import com.example.ui.components.TransactionType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.util.Date
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
        val uid = try { supabase.auth.currentSessionOrNull()?.user?.id } catch (e: Exception) { null } ?: return
        isPublishing = true
        
        val listingId = UUID.randomUUID().toString()
        
        val docData = ListingDto(
            id = listingId,
            ownerUid = uid,
            title = title,
            description = description,
            condition = condition,
            deliveryMode = deliveryMode,
            category = category,
            type = type?.name ?: "",
            price = price.toLongOrNull(),
            exchangeFor = exchangeFor,
            cashComplement = cashComplement.toLongOrNull(),
            country = country,
            city = city,
            neighborhood = neighborhood,
            status = "active",
            createdAt = Date().toString(),
            viewCount = 0,
            favoriteCount = 0
        )

        viewModelScope.launch {
            try {
                supabase.from("listings").insert(docData)
                isPublishing = false
                publishSuccess = true
            } catch (e: Exception) {
                isPublishing = false
            }
        }
    }
}
