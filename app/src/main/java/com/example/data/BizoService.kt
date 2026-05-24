package com.example.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class BizoService(
    private val client: HttpClient,
    private val sessionManager: SessionManager
) {
    private val baseUrl = "https://bizo.aiko.qzz.io/api/v1"

    private suspend fun getAuthToken(): String? = sessionManager.authToken.first()

    private suspend fun HttpRequestBuilder.auth() {
        getAuthToken()?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    // AUTH
    suspend fun login(email: String, password: String): AuthResponse {
        return client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to email, "password" to password))
        }.body()
    }

    suspend fun register(
        email: String,
        password: String,
        displayName: String,
        username: String?
    ): AuthResponse {
        return client.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "email" to email,
                "password" to password,
                "password_confirmation" to password,
                "display_name" to displayName,
                "username" to username
            ))
        }.body()
    }

    suspend fun logout() {
        client.post("$baseUrl/auth/logout") {
            auth()
        }
        sessionManager.clearSession()
    }

    // LISTINGS
    suspend fun getListings(
        category: String? = null,
        type: String? = null,
        city: String? = null
    ): PaginatedResponse<ListingResource> {
        return client.get("$baseUrl/listings") {
            parameter("category", category)
            parameter("type", type)
            parameter("city", city)
        }.body()
    }

    suspend fun getListingDetails(id: String): ApiResponse<ListingResource> {
        return client.get("$baseUrl/listings/$id").body()
    }

    suspend fun getMyListings(): PaginatedResponse<ListingResource> {
        return client.get("$baseUrl/my/listings") {
            auth()
        }.body()
    }

    // SEARCH
    suspend fun search(
        query: String,
        category: String? = null,
        city: String? = null,
        minPrice: Int? = null,
        maxPrice: Int? = null
    ): PaginatedResponse<ListingResource> {
        return client.get("$baseUrl/search") {
            parameter("q", query)
            parameter("category", category)
            parameter("city", city)
            parameter("min_price", minPrice)
            parameter("max_price", maxPrice)
        }.body()
    }

    // PROFILE
    suspend fun getProfile(): UserResource {
        return client.get("$baseUrl/profile") {
            auth()
        }.body()
    }

    suspend fun saveFcmToken(token: String): ApiResponse<Unit> {
        return client.post("$baseUrl/auth/fcm-token") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(mapOf("fcm_token" to token))
        }.body()
    }

    suspend fun updateProfile(params: Map<String, Any?>): UserResource {
        return client.put("$baseUrl/profile") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(params)
        }.body()
    }

    suspend fun uploadAvatar(avatarBytes: ByteArray, filename: String): UserResource {
        return client.submitFormWithBinaryData(
            url = "$baseUrl/profile/avatar",
            formData = formData {
                append("avatar", avatarBytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                })
            }
        ) {
            auth()
        }.body()
    }

    // FAVORITES
    suspend fun getFavorites(): PaginatedResponse<ListingResource> {
        return client.get("$baseUrl/favorites") {
            auth()
        }.body()
    }

    suspend fun toggleFavorite(listingId: String, isFavoriteNow: Boolean) {
        if (isFavoriteNow) {
            client.delete("$baseUrl/favorites/$listingId") {
                auth()
            }
        } else {
            client.post("$baseUrl/favorites/$listingId") {
                auth()
            }
        }
    }

    suspend fun createListing(
        title: String,
        description: String,
        type: String,
        price: Long?,
        category: String,
        condition: String,
        deliveryMode: String,
        country: String,
        city: String,
        neighborhood: String?,
        exchangeFor: String?,
        cashComplement: Long?
    ): ApiResponse<ListingResource> {
        return client.submitFormWithBinaryData(
            url = "$baseUrl/listings",
            formData = formData {
                append("title", title)
                append("description", description)
                append("type", type)
                if (price != null) append("price", price)
                append("category", category)
                append("condition", condition)
                append("delivery_mode", deliveryMode)
                append("country", country)
                append("city", city)
                if (neighborhood != null) append("neighborhood", neighborhood)
                if (exchangeFor != null) append("exchange_for", exchangeFor)
                if (cashComplement != null) append("cash_complement", cashComplement)
                // In a real app we would append photos here
            }
        ) {
            auth()
        }.body()
    }

    // CONVERSATIONS
    suspend fun getConversations(): PaginatedResponse<ConversationResource> {
        return client.get("$baseUrl/conversations") {
            auth()
        }.body()
    }

    suspend fun createConversation(listingId: String, message: String): ApiResponse<ConversationResource> {
        return client.post("$baseUrl/conversations") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(mapOf("listing_id" to listingId, "message" to message))
        }.body()
    }

    suspend fun getMessages(convId: String): PaginatedResponse<MessageResource> {
        return client.get("$baseUrl/conversations/$convId/messages") {
            auth()
        }.body()
    }

    suspend fun sendTextMessage(convId: String, text: String): MessageResource {
        return client.post("$baseUrl/conversations/$convId/messages") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(mapOf("type" to "text", "text" to text))
        }.body()
    }

    suspend fun markRead(convId: String) {
        client.post("$baseUrl/conversations/$convId/read") {
            auth()
        }
    }

    // TRANSACTIONS
    suspend fun createTransaction(
        listingId: String,
        buyerId: String,
        type: String,
        finalPrice: Int
    ): TransactionResource {
        return client.post("$baseUrl/transactions") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "listing_id" to listingId,
                "buyer_id" to buyerId,
                "type" to type,
                "final_price" to finalPrice
            ))
        }.body()
    }

    // REVIEWS
    suspend fun createReview(transactionId: String, rating: Int, comment: String?): ReviewResource {
        return client.post("$baseUrl/reviews") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "transaction_id" to transactionId,
                "rating" to rating,
                "comment" to comment
            ))
        }.body()
    }

    suspend fun getUserReviews(userId: String): PaginatedResponse<ReviewResource> {
        return client.get("$baseUrl/users/$userId/reviews").body()
    }
}

val apiClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        })
    }
    install(Logging) {
        level = LogLevel.INFO
    }
    defaultRequest {
        accept(ContentType.Application.Json)
    }
}
