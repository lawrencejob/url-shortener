package com.lawrencejob.orchestrator

import com.lawrencejob.api.*
import com.lawrencejob.persistence.*
import com.lawrencejob.service.*

class OrchestratorService(
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
        return when (val writeResult = redisService.writeAliasIfNotExists(shortAlias, request.fullUrl)) {
            is WriteResult.Success -> ShortenUrlResponse(shortAlias)
            is WriteResult.ExistsAlready -> {
                throw IllegalArgumentException("Alias already exists") // todo - change this exception type
            }
            is WriteResult.Error -> throw writeResult.cause
        }
    }

    suspend fun resolveAlias(alias: String): String {
        // try to read from Redis
        val readResult = redisService.readAlias(alias)
        return when (readResult) {
            is ReadResult.Found -> readResult.value
            is ReadResult.NotFound -> throw NoSuchElementException("Alias not found") 
            is ReadResult.Error -> throw readResult.cause
        }
    }

    suspend fun deleteAlias(alias: String): Boolean {
        val deleted = redisService.deleteAlias(alias)
        return when (deleted) {
            is DeleteResult.Deleted -> true
            is DeleteResult.NotFound -> false
            is DeleteResult.Error -> throw deleted.cause
        }
    }

    suspend fun listUrls(): List<UrlItem> {
        // TODO: Return all stored URL mappings
        throw NotImplementedError("List URLs logic not implemented yet.")
    }
}
