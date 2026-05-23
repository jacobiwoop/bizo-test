package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteDto(
    val listingId: String,
    val uid: String,
    val addedAt: String
)
