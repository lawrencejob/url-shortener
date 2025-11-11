package com.lawrencejob.plugins

import com.lawrencejob.persistence.RedisService
import com.lawrencejob.persistence.ShortUrlRepository
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import org.koin.dsl.module
import io.ktor.server.application.*
import org.koin.ktor.ext.get

/**
 * Provides RedisClient, connection, async commands, and RedisService
 * as Koin singletons.
 */
fun redisModule(application: Application) = module {
    single {
        val redisUrl = application.environment.config
            .propertyOrNull("ktor.redis.url")
            ?.getString() ?: "redis://localhost:6379"
        RedisClient.create(redisUrl)
    }

    single<StatefulRedisConnection<String, String>> {
        get<RedisClient>().connect()
    }

    single<RedisAsyncCommands<String, String>> {
        get<StatefulRedisConnection<String, String>>().async()
    }

    single<ShortUrlRepository> { RedisService(get()) }
}
