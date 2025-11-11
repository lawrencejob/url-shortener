package com.lawrencejob.util

import com.lawrencejob.module
import com.lawrencejob.persistence.DeleteResult
import com.lawrencejob.persistence.ReadResult
import com.lawrencejob.persistence.ShortUrlRepository
import com.lawrencejob.persistence.WriteResult
import com.lawrencejob.service.AliasGeneratorService
import io.ktor.server.testing.ApplicationTestBuilder
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val testJson = Json { ignoreUnknownKeys = true }

fun ApplicationTestBuilder.configureUrlShortenerApp(
    repository: ShortUrlRepository = InMemoryShortUrlRepository(),
    aliasGenerator: AliasGeneratorService = DeterministicAliasGeneratorService(),
): ShortUrlRepository {
    application {
        module(
            includeRedisModule = false,
            additionalModules = listOf(overrides(repository, aliasGenerator))
        )
    }
    return repository
}

private fun overrides(
    repository: ShortUrlRepository,
    aliasGenerator: AliasGeneratorService,
) = module {
    single<ShortUrlRepository> { repository }
    single<AliasGeneratorService> { aliasGenerator }
}

class DeterministicAliasGeneratorService : AliasGeneratorService() {
    private val counter = AtomicInteger(1)

    override fun generateAlias(): String {
        val index = counter.getAndIncrement()
        return "alias-" + index.toString().padStart(4, '0')
    }
}

class InMemoryShortUrlRepository : ShortUrlRepository {
    private val storage = linkedMapOf<String, String>()

    override suspend fun writeAliasIfNotExists(key: String, value: String): WriteResult {
        if (storage.containsKey(key)) return WriteResult.ExistsAlready
        storage[key] = value
        return WriteResult.Success
    }

    override suspend fun readAlias(key: String): ReadResult {
        val value = storage[key] ?: return ReadResult.NotFound
        return ReadResult.Found(value)
    }

    override suspend fun deleteAlias(key: String): DeleteResult {
        return if (storage.remove(key) != null) {
            DeleteResult.Deleted
        } else {
            DeleteResult.NotFound
        }
    }

    override fun listAllAliasesFlow(): Flow<Pair<String, String>> = flow {
        storage.forEach { emit(it.key to it.value) }
    }
}
