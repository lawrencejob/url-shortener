package com.lawrencejob.orchestrator

import com.lawrencejob.api.*
import com.lawrencejob.persistence.*
import com.lawrencejob.service.*
import io.ktor.server.config.*
import kotlinx.coroutines.flow.map

class OrchestratorService(
    private val aliasGenerator: AliasGeneratorService,
    private val urlFilter: UrlFilterService,
    private val shortUrlRepository: ShortUrlRepository,
    private val appConfig: ApplicationConfig,
) {
    /**
     * Creates a short URL by validating the request, generating/using an alias and attempting
     * to persist it. Consumers get back a [CreateShortUrlResult] describing success or failure.
     */
    suspend fun createShortUrl(request: ShortenUrlRequest): CreateShortUrlResult {
        if (!urlFilter.isUrlAllowed(request.fullUrl)) {
            return CreateShortUrlResult.UrlNotAllowed("URL is not allowed")
        }

        request.customAlias?.let { alias ->
            if (alias.length !in 4..32) {
                return CreateShortUrlResult.InvalidAlias("Custom alias must be between 4 and 32 characters")
            }
        }

        val shortAlias = request.customAlias ?: aliasGenerator.generateAlias()
        val shortUrl = "${basePath()}/$shortAlias"

        return when (val writeResult = shortUrlRepository.writeAliasIfNotExists(shortAlias, request.fullUrl)) {
            is WriteResult.Success -> CreateShortUrlResult.Created(ShortenUrlResponse(shortUrl))
            is WriteResult.ExistsAlready -> CreateShortUrlResult.AliasUnavailable(shortAlias)
            is WriteResult.Error -> CreateShortUrlResult.Failure(writeResult.cause)
        }
    }

    /**
     * Resolves an alias into its destination URL, returning a [ResolveAliasResult]
     * instead of throwing so that HTTP routes can tailor responses.
     */
    suspend fun resolveAlias(alias: String): ResolveAliasResult =
        when (val readResult = shortUrlRepository.readAlias(alias)) {
            is ReadResult.Found -> ResolveAliasResult.Found(readResult.value)
            is ReadResult.NotFound -> ResolveAliasResult.NotFound(alias)
            is ReadResult.Error -> ResolveAliasResult.Failure(readResult.cause)
        }

    /**
     * Attempts to delete an alias and reports what happened through [DeleteAliasResult].
     */
    suspend fun deleteAlias(alias: String): DeleteAliasResult =
        when (val deleted = shortUrlRepository.deleteAlias(alias)) {
            is DeleteResult.Deleted -> DeleteAliasResult.Deleted
            is DeleteResult.NotFound -> DeleteAliasResult.NotFound
            is DeleteResult.Error -> DeleteAliasResult.Failure(deleted.cause)
        }

    /**
     * Streams every stored alias for listing endpoints.
     */
    fun listUrlsFlow() = shortUrlRepository
        .listAllAliasesFlow()
        .map { (alias, fullUrl) ->
            UrlItem(
                alias = alias,
                fullUrl = fullUrl,
                shortUrl = "${basePath()}/$alias"
            )
        }

    private fun basePath(): String =
        appConfig.propertyOrNull("ktor.basePath")?.getString() ?: "http://localhost:8080"
}
