package com.lawrencejob.orchestrator

import com.lawrencejob.api.*
import com.lawrencejob.persistence.*
import com.lawrencejob.service.*

class UrlShortenerService(
    private val aliasGenerator: AliasGeneratorService,
    private val urlFilter: UrlFilterService,
    private val redisService: RedisService,
    ) {
    suspend fun createShortUrl(request: ShortenUrlRequest): ShortenUrlResponse {

        // check that the URL is allowed (e.g., not in a blocklist)
        if (urlFilter.isUrlAllowed(request.fullUrl) == false) {
            throw IllegalArgumentException("URL is not allowed")
        }

        // if they have specified an alias:
        if (request.customAlias != null) {
            // check that the alias follows basic rules (e.g., length, characters)
            // todo
            if (request.customAlias.length < 4 || request.customAlias.length > 32) {
                throw IllegalArgumentException("Custom alias must be between 4 and 32 characters")
            }
        }

        // if the alias is valid, use it; otherwise, generate one
        val shortAlias = request.customAlias ?: aliasGenerator.generateAlias()

        // try to write, handle possible outcomes
        when (val writeResult = redisService.writeIfNotExists(shortAlias, request.fullUrl)) {
            is WriteResult.Success -> {
                return ShortenUrlResponse(shortAlias)
            }
            is WriteResult.ExistsAlready -> {
                throw IllegalArgumentException("Alias already exists") // todo - change this exception type
            }
            is WriteResult.Error -> {
                throw writeResult.cause
            }
        }
    }

    suspend fun resolveAlias(alias: String): String {
        // TODO: Lookup alias and return the original full URL or throw if missing
        throw NotImplementedError("Alias resolution logic not implemented yet.")
    }

    suspend fun deleteAlias(alias: String): Boolean {
        // TODO: Delete alias and return true when deletion succeeds
        throw NotImplementedError("Alias deletion logic not implemented yet.")
    }

    suspend fun listUrls(): List<UrlItem> {
        // TODO: Return all stored URL mappings
        throw NotImplementedError("List URLs logic not implemented yet.")
    }
}
