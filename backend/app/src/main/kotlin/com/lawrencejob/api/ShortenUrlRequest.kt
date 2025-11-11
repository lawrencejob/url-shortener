package com.lawrencejob.api

import kotlinx.serialization.Serializable

@Serializable
data class ShortenUrlRequest(
    val fullUrl: String,
    val customAlias: String? = null,
)

@Serializable
data class ShortenUrlResponse(
    val shortUrl: String,
)

@Serializable
data class UrlItem(
    val alias: String,
    val fullUrl: String,
    val shortUrl: String,
)