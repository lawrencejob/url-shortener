package com.lawrencejob.service

class UrlFilterService {
    @Suppress("UNUSED_PARAMETER")
    fun isUrlAllowed(url: String): Boolean {
        return url.startsWith("https://")
    }
}