package com.lawrencejob.routes

import com.lawrencejob.util.InMemoryShortUrlRepository
import com.lawrencejob.util.configureUrlShortenerApp
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class AliasRoutesTest {

    @Test
    fun `GET alias responds with redirect`() = testApplication {
        val repository = InMemoryShortUrlRepository().also {
            runBlocking { it.writeAliasIfNotExists("alias1", "https://example.com") }
        }

        configureUrlShortenerApp(repository = repository)

        val response = client.config {
            followRedirects = false
        }.get("/alias1")

        assertEquals(HttpStatusCode.Found, response.status)
        assertEquals("https://example.com", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `DELETE alias reports presence`() = testApplication {
        val repository = InMemoryShortUrlRepository().also {
            runBlocking { it.writeAliasIfNotExists("alias1", "https://example.com") }
        }

        configureUrlShortenerApp(repository = repository)

        val firstDelete = client.delete("/alias1")
        assertEquals(HttpStatusCode.NoContent, firstDelete.status)

        val secondDelete = client.delete("/alias1")
        assertEquals(HttpStatusCode.NotFound, secondDelete.status)
    }
}
