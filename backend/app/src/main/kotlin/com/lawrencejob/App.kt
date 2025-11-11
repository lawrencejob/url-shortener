package com.lawrencejob

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.config.*
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.core.module.Module

import com.lawrencejob.orchestrator.*
import com.lawrencejob.api.*
import com.lawrencejob.service.*
import com.lawrencejob.persistence.ShortUrlRepository
import com.lawrencejob.plugins.redisModule

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module(
    includeRedisModule: Boolean = true,
    additionalModules: List<Module> = emptyList(),
) {
    log.info("Loaded config entries:")
    environment.config.toMap().forEach { (k, v) -> log.info("$k = $v") }
    val appConfig = environment.config
    
    val appModule = module {
        single<ApplicationConfig> { appConfig }
        single { UrlFilterService() }
        single { AliasGeneratorService() }
        single { OrchestratorService(get(), get(), get<ShortUrlRepository>(), get()) }
    }

    val moduleList = mutableListOf(appModule)
    if (includeRedisModule) {
        moduleList += redisModule(this@module)
    }
    moduleList += additionalModules

    install(Koin) {
        slf4jLogger()
        allowOverride(true)
        modules(*moduleList.toTypedArray())
    }

    install(ContentNegotiation) {
        json()
    }

    if (includeRedisModule) {
        // this shuts down Redis - see redisModule.kt
        // in ktor 4, this can move to the module's 'onStop'
        monitor.subscribe(ApplicationStopped) {
            val koin = getKoin()
            val client = koin.get<RedisClient>()
            val connection = koin.get<StatefulRedisConnection<String, String>>()
            connection.close()
            client.shutdown()
        }
    }

    // --- Routes ---
    routing {
        val orchestrator by inject<OrchestratorService>()

        post("/shorten") {
            val request = call.receive<ShortenUrlRequest>()
            val result = orchestrator.createShortUrl(request)
            call.respond(HttpStatusCode.Created, result)
        }

        route("/{alias}") {
            get {
                val alias = call.parameters.getOrFail("alias")
                val destination = orchestrator.resolveAlias(alias)
                call.respondRedirect(destination, permanent = false)
            }

            delete {
                val alias = call.parameters.getOrFail("alias")
                val removed = orchestrator.deleteAlias(alias)

                // use restful status codes
                if (removed) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }

        get("/urls") {
            // use a text stream to avoid building the entire list in memory - not sure if idiomatic
            call.respondTextWriter(contentType = ContentType.Application.Json) {
                write("[")
                var first = true
                orchestrator.listUrlsFlow().collect { item ->
                    if (!first) write(",") else first = false
                    write("""{"alias":"${item.alias}","fullUrl":"${item.fullUrl}","shortUrl":"${item.shortUrl}"}""")
                    flush()
                }
                write("]")
            }
        }
    }
}
