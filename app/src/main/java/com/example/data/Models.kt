package com.example.data

import com.example.ui.components.TransactionType

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
    val rating: Double? = 0.0,
    val review_count: Int? = 0,
    val total_sales: Int? = 0,
    val is_verified: Boolean? = false,
    val has_seen_onboarding: Boolean? = false,
    val created_at: String
)

@Serializable
data class PartialUser(
    val id: String,
    val display_name: String,
    val photo_url: String? = null,
    val last_seen_at: String? = null
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
    val photos: List<String> = emptyList(),
    val country: String,
    val city: String,
    val neighborhood: String? = null,
    val tags: List<String> = emptyList(),
    val view_count: Int? = 0,
    val favorite_count: Int? = 0,
    val status: String? = null,
    val is_boosted: Boolean? = false,
    val expires_at: String? = null,
    val created_at: String,
    val updated_at: String,
    val owner: UserResource? = null
)

@Serializable
data class PlainMessageResponse(
    val message: String
)

@Serializable
data class ApiResponse<T>(
    val data: T,
    val message: String? = null
)

@Serializable
data class MetaLinkResource(
    val url: String? = null,
    val label: String? = null,
    val active: Boolean? = null
)

@Serializable
data class MetaResource(
    val current_page: Int? = null,
    val from: Int? = null,
    val last_page: Int? = null,
    val links: List<MetaLinkResource>? = null,
    val path: String? = null,
    val per_page: Int? = null,
    val to: Int? = null,
    val total: Int? = null
)

@Serializable
data class LinksResource(
    val first: String? = null,
    val last: String? = null,
    val prev: String? = null,
    val next: String? = null
)

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val links: LinksResource? = null,
    val meta: MetaResource? = null
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResource
)

@Serializable
data class ConversationResource(
    val id: String,
    val listing_id: String,
    val listing_title: String,
    val listing_photo: String? = null,
    val last_message: String? = null,
    val last_message_at: String? = null,
    val unread_count: Int = 0,
    val other_user: PartialUser? = null,
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
    val proposal: TrocProposal? = null,
    val is_read: Boolean = false,
    val created_at: String
)

@Serializable
data class TrocProposal(
    val offered_listing_id: String,
    val offered_listing_title: String,
    val offered_listing_photo: String? = null,
    val cash_amount: Int? = null,
    val status: String,
    val refusal_reason: String? = null
)

@Serializable
data class FavoriteResource(
    val id: String,
    val user_id: String,
    val listing_id: String,
    val listing_title: String,
    val listing_photo: String? = null,
    val listing_price: Long? = null,
    val listing_type: String,
    val created_at: String,
    val listing: ListingResource? = null
)

@Serializable
data class TransactionResource(
    val id: String,
    val listing_id: String,
    val seller_id: String,
    val buyer_id: String,
    val type: String,
    val final_price: Int,
    val seller_reviewed: Boolean = false,
    val buyer_reviewed: Boolean = false,
    val created_at: String
)

@Serializable
data class ReviewResource(
    val id: String,
    val transaction_id: String,
    val rating: Int,
    val comment: String? = null,
    val created_at: String,
    val author: UserResource? = null,
    val from_uid: String? = null,
    val to_uid: String? = null,
    val listing_id: String? = null
)

@Serializable
data class ErrorResponse(
    val message: String,
    val errors: Map<String, List<String>>? = null
)

@Serializable
data class CreateConversationResponse(
    val data: ConversationResource,
    val message: MessageResource
)

@Serializable
data class NotificationResource(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val data: Map<String, String>? = null,
    val is_read: Boolean = false,
    val created_at: String
)

data class PickedImage(
    val uri: String,
    val bytes: ByteArray,
    val mimeType: String,
    val filename: String
)

data class Product(
    val id: String,
    val title: String,
    val price: String, // String for simplicity e.g. "185 000 FCFA"
    val location: String,
    val imageUrl: String,
    val type: TransactionType,
    val sellerName: String,
    val timeAgo: String,
    val description: String = "",
    val category: String = "",
    val condition: String = "",
    val deliveryMode: String = "",
    val photos: List<String> = emptyList()
)

val mockProducts = listOf(
    Product("1", "iPhone 13", "185 000 FCFA", "Cotonou, Akpakpa", "https://images.unsplash.com/photo-1632661674596-df8be070a5c5?auto=format&fit=crop&q=80&w=400", TransactionType.VENTE, "Kouassi A.", "il y a 2 heures"),
    Product("2", "Mountain Bike", "Cherche smartphone", "Cotonou, Cadjehoun", "https://images.unsplash.com/photo-1532298229144-0ec0c57515c7?auto=format&fit=crop&q=80&w=400", TransactionType.TROC, "Aïcha", "il y a 1 jour"),
    Product("3", "Table en bois", "45 000 FCFA", "Cotonou, Godomey", "https://images.unsplash.com/photo-1577140917170-285929fb55b7?auto=format&fit=crop&q=80&w=400", TransactionType.TROC_CASH, "Désiré", "il y a 3 jours"),
    Product("4", "Robe wax taille M", "Échange habits", "Porto-Novo", "https://images.unsplash.com/photo-1515347619152-1cd9e414f5ec?auto=format&fit=crop&q=80&w=400", TransactionType.TROC, "Fatoumata", "Hier")
)
