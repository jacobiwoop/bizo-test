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
    @GET("listings")
    suspend fun getListings(@Query("category") category: String? = null): PaginatedResponse<ListingResource>

    @GET("listings/{id}")
    suspend fun getListing(@Path("id") id: String): DataResponse<ListingResource>

    @GET("conversations")
    suspend fun getConversations(): PaginatedResponse<ConversationResource>

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
    ): DataResponse<ConversationResource>

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
                    chain.proceed(request.build())
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
