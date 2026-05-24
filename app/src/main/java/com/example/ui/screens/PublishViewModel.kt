package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BizoService
import com.example.data.PickedImage
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

    // Multiple photos
    val selectedPhotos = mutableStateListOf<PickedImage>()

    var currentStep by mutableStateOf(1)
    var isPublishing by mutableStateOf(false)
    var publishSuccess by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun addPhoto(photo: PickedImage) {
        if (selectedPhotos.size < 10) {
            selectedPhotos.add(photo)
        }
    }

    fun removePhoto(photo: PickedImage) {
        selectedPhotos.remove(photo)
    }

    private fun validate(): Boolean {
        if (selectedPhotos.isEmpty()) {
            errorMessage = "Au moins une photo est requise."
            currentStep = 1
            return false
        }
        if (title.length < 5 || title.length > 80) {
            errorMessage = "Le titre doit faire entre 5 et 80 caractères."
            currentStep = 2
            return false
        }
        if (description.length < 20 || description.length > 500) {
            errorMessage = "La description doit faire entre 20 et 500 caractères."
            currentStep = 2
            return false
        }
        if (type == null) {
            errorMessage = "Le type de transaction est requis."
            currentStep = 4
            return false
        }
        if (type == TransactionType.VENTE && price.isEmpty()) {
            errorMessage = "Le prix est requis pour une vente."
            currentStep = 4
            return false
        }
        if ((type == TransactionType.TROC || type == TransactionType.TROC_CASH) && exchangeFor.isEmpty()) {
            errorMessage = "L'objet d'échange est requis pour un troc."
            currentStep = 4
            return false
        }
        return true
    }

    fun publish() {
        errorMessage = null
        if (!validate()) return

        isPublishing = true
        
        viewModelScope.launch {
            try {
                // Mapping category
                val apiCategory = when (category) {
                    "Électronique" -> "electronique"
                    "Mode" -> "mode"
                    "Maison" -> "maison"
                    else -> category.lowercase()
                }

                bizoService.createListing(
                    title = title,
                    description = description,
                    type = type?.name ?: "VENTE",
                    price = price.toLongOrNull(),
                    category = apiCategory,
                    condition = condition.lowercase(),
                    deliveryMode = when(deliveryMode) {
                        "Main propre" -> "main_propre"
                        "Livraison" -> "livraison"
                        "Les deux" -> "les_deux"
                        else -> deliveryMode.lowercase()
                    },
                    country = "BJ", // Default to Benin
                    city = city,
                    neighborhood = neighborhood,
                    exchangeFor = exchangeFor,
                    cashComplement = cashComplement.toLongOrNull(),
                    photos = selectedPhotos.toList()
                )
                isPublishing = false
                publishSuccess = true
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = e.message ?: "Une erreur inattendue est survenue."
                isPublishing = false
            }
        }
    }
}
