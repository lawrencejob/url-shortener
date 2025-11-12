package com.lawrencejob.orchestrator

import com.lawrencejob.api.ShortenUrlResponse

sealed interface CreateShortUrlResult {
    data class Created(val response: ShortenUrlResponse) : CreateShortUrlResult
    data class AliasUnavailable(val alias: String) : CreateShortUrlResult
    data class InvalidAlias(val reason: String) : CreateShortUrlResult
    data class UrlNotAllowed(val reason: String) : CreateShortUrlResult
    data class Failure(val cause: Throwable) : CreateShortUrlResult
}

sealed interface ResolveAliasResult {
    data class Found(val destination: String) : ResolveAliasResult
    data class NotFound(val alias: String) : ResolveAliasResult
    data class Failure(val cause: Throwable) : ResolveAliasResult
}

sealed interface DeleteAliasResult {
    object Deleted : DeleteAliasResult
    object NotFound : DeleteAliasResult
    data class Failure(val cause: Throwable) : DeleteAliasResult
}
