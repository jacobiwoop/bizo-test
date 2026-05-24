package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BizoService
import com.example.ui.components.TransactionType
import kotlinx.coroutines.launch

class PublishViewModel(private val bizoService: BizoService) : ViewModel() {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var condition by mutableStateOf("Bon")
    var deliveryMode by mutableStateOf("Les deux")
    var category by mutableStateOf("Électronique")
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
        isPublishing = true
        
        viewModelScope.launch {
            try {
                bizoService.createListing(
                    title = title,
                    description = description,
                    type = type?.name ?: "VENTE",
                    price = price.toLongOrNull(),
                    category = category,
                    condition = condition.lowercase(),
                    deliveryMode = deliveryMode.lowercase().replace(" ", "_"),
                    country = country,
                    city = city,
                    neighborhood = neighborhood,
                    exchangeFor = exchangeFor,
                    cashComplement = cashComplement.toLongOrNull()
                )
                isPublishing = false
                publishSuccess = true
            } catch (e: Exception) {
                e.printStackTrace()
                isPublishing = false
            }
        }
    }
}
