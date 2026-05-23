package com.example.data

import com.example.ui.components.TransactionType

data class Product(
    val id: String,
    val title: String,
    val price: String, // String for simplicity e.g. "185 000 FCFA"
    val location: String,
    val imageUrl: String,
    val type: TransactionType,
    val sellerName: String,
    val timeAgo: String
)

val mockProducts = listOf(
    Product("1", "iPhone 13", "185 000 FCFA", "Cotonou, Akpakpa", "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400", TransactionType.VENTE, "Kouassi A.", "il y a 2 heures"),
    Product("2", "Mountain Bike", "Cherche smartphone", "Cotonou, Cadjehoun", "https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?auto=format&fit=crop&q=80&w=400", TransactionType.TROC, "Aïcha", "il y a 1 jour"),
    Product("3", "Table en bois", "45 000 FCFA", "Cotonou, Godomey", "https://images.unsplash.com/photo-1577140917170-285929fb55b7?auto=format&fit=crop&q=80&w=400", TransactionType.TROC_CASH, "Désiré", "il y a 3 jours"),
    Product("4", "Robe wax taille M", "Échange habits", "Porto-Novo", "https://images.unsplash.com/photo-1515347619152-1cd9e414f5ec?auto=format&fit=crop&q=80&w=400", TransactionType.TROC, "Fatoumata", "Hier")
)
