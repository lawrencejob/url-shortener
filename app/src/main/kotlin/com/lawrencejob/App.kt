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
import org.koin.ktor.ext.*
import org.koin.dsl.module
import org.koin.logger.slf4jLogger
import org.koin.ktor.plugin.Koin

import com.lawrencejob.orchestrator.*
import com.lawrencejob.api.*
import com.lawrencejob.service.*

fun main() {
    val appModule = module {
        single { UrlShortenerService(get(), get(), get()) }
        single { EncoderService() }
        single { UrlFilterService() }
        single { RedisService(get()) }
        single { AliasGeneratorService() }
    }

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }
        
        install(Koin) {
            modules(appModule)
            slf4jLogger()
        }

        routing {
            val urlShortenerService by inject<UrlShortenerService>()

            post("/shorten") {
                val request = call.receive<ShortenUrlRequest>()
                val result = urlShortenerService.createShortUrl(request)
                call.respond(HttpStatusCode.Created, result)
            }

            route("/{alias}") {
                get {
                    val alias = call.parameters.getOrFail("alias")
                    val destination = urlShortenerService.resolveAlias(alias)
                    call.respondRedirect(destination, permanent = false)
                }

                delete {
                    val alias = call.parameters.getOrFail("alias")
                    val removed = urlShortenerService.deleteAlias(alias)
                    if (removed) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            get("/urls") {
                val urls = urlShortenerService.listUrls()
                call.respond(urls)
            }
        }
    }.start(wait = true)
}


