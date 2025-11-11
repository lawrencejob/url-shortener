package com.lawrencejob.persistence

import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.RedisException
import io.lettuce.core.SetArgs
import kotlinx.coroutines.future.await

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

class RedisService(
    private val redis: RedisAsyncCommands<String, String>
) {
    /**
     * Suspends while writing to Redis with NX (only if not exists).
     * Returns Success, ExistsAlready, or Error.
     */
    suspend fun writeIfNotExists(key: String, value: String): WriteResult {
        return try {
            val result = redis.set(key, value, SetArgs().nx()).await()
            when (result) {
                "OK" -> WriteResult.Success
                null  -> WriteResult.ExistsAlready
                else  -> WriteResult.Error(IllegalStateException("Unexpected Redis reply: $result"))
            }
        } catch (ex: RedisException) {
            WriteResult.Error(ex)
        }
    }

    suspend fun read(key: String): ReadResult {
        return try {
            val value = redis.get(key).await()
            when (value) {
                null -> ReadResult.NotFound
                else -> ReadResult.Found(value)
            }
        } catch (ex: RedisException) {
            ReadResult.Error(ex)
        }
    }
}