package com.lawrencejob.orchestrator

import com.lawrencejob.api.*
import com.lawrencejob.persistence.*
import com.lawrencejob.service.*
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.config.*

import kotlinx.coroutines.flow.map


class OrchestratorService(
    private val aliasGenerator: AliasGeneratorService,
    private val urlFilter: UrlFilterService,
    private val redisService: RedisService,
    private val appConfig: ApplicationConfig,
    ) {
    suspend fun createShortUrl(request: ShortenUrlRequest): ShortenUrlResponse {

        // check that the URL is allowed (e.g., not in a blocklist)
        if (urlFilter.isUrlAllowed(request.fullUrl) == false) {
            throw BadRequestException("URL is not allowed")
        }

        // if they have specified an alias:
        if (request.customAlias != null) {
            // check that the alias follows basic rules (e.g., length, characters)
            // todo
            if (request.customAlias.length < 4 || request.customAlias.length > 32) {
                throw BadRequestException("Custom alias must be between 4 and 32 characters")
            }
        }

        // if the alias is valid, use it; otherwise, generate one
        val shortAlias = request.customAlias ?: aliasGenerator.generateAlias()
        
        val basePath = appConfig.propertyOrNull("ktor.basePath")?.getString() ?: "http://localhost:8080"

        // try to write, handle possible outcomes
        return when (val writeResult = redisService.writeAliasIfNotExists(shortAlias, request.fullUrl)) {
            is WriteResult.Success -> ShortenUrlResponse("$basePath/$shortAlias")
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
            is ReadResult.NotFound -> throw NotFoundException("Alias not found") 
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

    fun listUrlsFlow() = redisService
    .listAllAliasesFlow()
    .map { (alias, fullUrl) ->
        val basePath = appConfig.propertyOrNull("ktor.basePath")?.getString()
            ?: "http://localhost:8080"
        UrlItem(
            alias = alias,
            fullUrl = fullUrl,
            shortUrl = "$basePath/$alias"
        )
    }
}
