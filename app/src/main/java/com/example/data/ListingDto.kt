package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class ListingDto(
    val id: String? = null,
    val title: String,
    val price: Long?,
    val city: String,
    val neighborhood: String?,
    val type: String,
    val ownerUid: String,
    val status: String,
    val description: String? = null,
    val condition: String? = null,
    val deliveryMode: String? = null,
    val category: String? = null,
    val exchangeFor: String? = null,
    val cashComplement: Long? = null,
    val country: String? = null,
    val createdAt: String? = null,
    val viewCount: Int? = 0,
    val favoriteCount: Int? = 0
)
