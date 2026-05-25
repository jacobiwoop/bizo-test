package com.example.data.api

import com.example.data.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.*
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

interface BizoService {
    @POST("auth/login")
    suspend fun login(@Body body: Map<String, String>): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body body: Map<String, String>): AuthResponse

    @POST("auth/logout")
    suspend fun logout(): Map<String, String>

    @GET("favorites")
    suspend fun getFavorites(): PaginatedResponse<FavoriteResource>

    @POST("favorites/{listingId}")
    suspend fun addFavorite(@Path("listingId") listingId: String): Map<String, String>

    @DELETE("favorites/{listingId}")
    suspend fun removeFavorite(@Path("listingId") listingId: String): Map<String, String>

    @GET("listings")
    suspend fun getListings(@Query("category") category: String? = null): PaginatedResponse<ListingResource>

    @GET("listings/{id}")
    suspend fun getListing(@Path("id") id: String): DataResponse<ListingResource>

    @GET("conversations")
    suspend fun getConversations(): PaginatedResponse<ConversationResource>

    @GET("conversations/{id}")
    suspend fun getConversation(@Path("id") id: String): DataResponse<ConversationResource>

    @GET("conversations/{id}/messages")
    suspend fun getMessages(@Path("id") id: String): PaginatedResponse<MessageResource>

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): DataResponse<MessageResource>

    @POST("conversations")
    suspend fun createConversation(
        @Body body: Map<String, String>
    ): CreateConversationResponse

    @POST("listings")
    suspend fun createListing(@Body body: ListingRequest): DataResponse<ListingResource>

    @DELETE("listings/{id}")
    suspend fun deleteListing(@Path("id") id: String): Map<String, String>

    @PUT("listings/{id}")
    suspend fun updateListing(@Path("id") id: String, @Body body: ListingRequest): DataResponse<ListingResource>

    @POST("debug-logs")
    suspend fun sendDebugLogs(@Body body: DebugLogsRequest): SendDebugLogsResponse

    @GET("debug-logs/history")
    suspend fun getDebugLogsHistory(): DataResponse<List<DebugLogHistoryItem>>

    @GET("profile")
    suspend fun getProfile(): DataResponse<UserResource>

    @GET("my/listings")
    suspend fun getMyListings(): PaginatedResponse<ListingResource>

    companion object {
        private const val BASE_URL = "https://bizo.aiko.qzz.io/api/v1/"

        fun create(sessionManager: SessionManager): BizoService {
            val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .addInterceptor { chain ->
                    val token = sessionManager.getAuthToken()
                    val request = chain.request().newBuilder()
                    if (token != null) {
                        request.addHeader("Authorization", "Bearer $token")
                    }
                    val response = chain.proceed(request.build())
                    
                    if (!response.isSuccessful) {
                        DebugLogger.error(
                            LogCategory.ERROR, 
                            "Erreur HTTP ${response.code}", 
                            "URL: ${chain.request().url}, Message: ${response.message}"
                        )

                        if (response.code == 401) {
                            DebugLogger.warn(LogCategory.AUTH, "Session expirée", "Nettoyage local après 401")
                            sessionManager.clearSession()
                        }
                    }
                    
                    response
                }
                .build()

            val json = Json { ignoreUnknownKeys = true }
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(BizoService::class.java)
        }
    }
}
