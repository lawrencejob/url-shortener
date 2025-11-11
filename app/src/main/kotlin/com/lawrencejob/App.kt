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
import kotlinx.serialization.Serializable
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

import com.lawrencejob.orchestrator.*
import com.lawrencejob.api.*
import com.lawrencejob.service.*
import com.lawrencejob.persistence.RedisService
import com.lawrencejob.plugins.redisModule

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    
    val appModule = module {
        single { OrchestratorService(get(), get(), get()) }
        single { UrlFilterService() }
        single { AliasGeneratorService() }
        // todo - what did I forget
    }

    install(Koin) {
        slf4jLogger()
        modules(appModule, redisModule(this@module))
    }

    install(ContentNegotiation) {
        json()
    }

    // this shuts down Redis - see redisModule.kt
    // in ktor 4, this can move to the module's 'onStop'
    monitor.subscribe(ApplicationStopped) {
        val koin = getKoin()
        val client = koin.get<RedisClient>()
        val connection = koin.get<StatefulRedisConnection<String, String>>()
        connection.close()
        client.shutdown()
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
            val urls = orchestrator.listUrls()
            call.respond(urls)
        }
    }
}