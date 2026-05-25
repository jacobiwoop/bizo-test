package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class UserResource(
    val id: String,
    val email: String,
    val display_name: String,
    val username: String? = null,
    val photo_url: String? = null,
    val bio: String? = null,
    val country_code: String? = null,
    val rating: Float? = null,
    val review_count: Int? = null,
    val total_sales: Int? = null,
    val is_verified: Boolean? = null,
    val has_seen_onboarding: Boolean? = null,
    val created_at: String
)

@Serializable
data class ListingResource(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val price: Long? = null,
    val cash_complement: Long? = null,
    val exchange_for: String? = null,
    val category: String,
    val condition: String,
    val delivery_mode: String,
    val photos: List<String>,
    val country: String,
    val city: String,
    val neighborhood: String? = null,
    val view_count: Int,
    val favorite_count: Int,
    val status: String,
    val is_boosted: Boolean,
    val owner: UserResource? = null,
    val created_at: String
)

@Serializable
data class ConversationResource(
    val id: String,
    val listing_id: String,
    val listing_title: String,
    val listing_photo: String? = null,
    val last_message: String? = null,
    val last_message_at: String? = null,
    val unread_count: Int,
    val other_user: UserResource,
    val created_at: String
)

@Serializable
data class MessageResource(
    val id: String,
    val conv_id: String,
    val sender_id: String,
    val type: String,
    val text: String? = null,
    val image_url: String? = null,
    val created_at: String
)

@Serializable
data class FavoriteResource(
    val id: String,
    val listing_id: String,
    val listing_title: String? = null,
    val listing_photo: String? = null,
    val created_at: String
)

@Serializable
data class CreateConversationResponse(
    val data: ConversationResource,
    val message: MessageResource
)

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>
)

@Serializable
data class DataResponse<T>(
    val data: T
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResource
)
