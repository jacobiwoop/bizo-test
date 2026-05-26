package com.example.data

object MediaUrlResolver {
    private const val API_HOST = "https://bizo.aiko.qzz.io"

    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("http")) url else "$API_HOST$url"
    }
}
