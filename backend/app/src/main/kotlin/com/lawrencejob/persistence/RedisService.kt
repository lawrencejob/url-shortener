package com.lawrencejob.persistence

import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.RedisException
import io.lettuce.core.SetArgs
import kotlinx.coroutines.future.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


sealed class WriteResult {
    object Success : WriteResult()
    object ExistsAlready : WriteResult()
    data class Error(val cause: Throwable) : WriteResult()
}

sealed class ReadResult {
    data class Found(val value: String) : ReadResult()
    object NotFound : ReadResult()
    data class Error(val cause: Throwable) : ReadResult()
}

sealed class DeleteResult {
    object Deleted : DeleteResult()
    object NotFound : DeleteResult()
    data class Error(val cause: Throwable) : DeleteResult()
}

interface ShortUrlRepository {
    suspend fun writeAliasIfNotExists(key: String, value: String): WriteResult
    suspend fun readAlias(key: String): ReadResult
    suspend fun deleteAlias(key: String): DeleteResult
    fun listAllAliasesFlow(): Flow<Pair<String, String>>
}

class RedisService(
    private val redis: RedisAsyncCommands<String, String>
) : ShortUrlRepository {

    override suspend fun writeAliasIfNotExists(key: String, value: String): WriteResult {
        if (!validateAlias(key)) {
            return WriteResult.Error(IllegalArgumentException("Alias contains invalid characters"))
        }
        return try {
            val result = redis.set("alias:{$key}", value, SetArgs().nx()).await()
            when (result) {
                "OK" -> WriteResult.Success
                null  -> WriteResult.ExistsAlready
                else  -> WriteResult.Error(IllegalStateException("Unexpected Redis reply: $result"))
            }
        } catch (ex: RedisException) {
            WriteResult.Error(ex)
        }
    }

    override suspend fun readAlias(key: String): ReadResult {
        if (!validateAlias(key)) {
            return ReadResult.Error(IllegalArgumentException("Alias contains invalid characters"))
        }
        return try {
            val value = redis.get("alias:{$key}").await()
            when (value) {
                null -> ReadResult.NotFound
                else -> ReadResult.Found(value)
            }
        } catch (ex: RedisException) {
            ReadResult.Error(ex)
        }
    }

    override suspend fun deleteAlias(key: String): DeleteResult {
        if (!validateAlias(key)) {
            return DeleteResult.Error(IllegalArgumentException("Alias contains invalid characters"))
        }
        return try {
            val deletedCount = redis.del("alias:{$key}").await()
            if (deletedCount > 0) {
                DeleteResult.Deleted
            } else {
                DeleteResult.NotFound
            }
        } catch (ex: RedisException) {
            DeleteResult.Error(ex)
        }
    }

    override fun listAllAliasesFlow(): Flow<Pair<String, String>> = flow {
        val keys = redis.keys("alias:*").await()
        for (key in keys) {
            val value = redis.get(key).await()
            if (value != null) {
                emit(key.removePrefix("alias:") to value)
            }
        }
    }

    private fun validateAlias(alias: String): Boolean {
        // only permit alphanumerics
        val regex = "^[a-zA-Z0-9]+$".toRegex()
        return regex.matches(alias)
    }
}
