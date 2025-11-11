package com.lawrencejob.routes

import com.lawrencejob.api.ShortenUrlRequest
import com.lawrencejob.api.ShortenUrlResponse
import com.lawrencejob.persistence.ReadResult
import com.lawrencejob.util.configureUrlShortenerApp
import com.lawrencejob.util.testJson
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class ShortenRoutesTest {

    @Test
    fun `custom alias returns 201`() = testApplication {
        configureUrlShortenerApp()

        val response = client.post("/shorten") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(ShortenUrlRequest(
                fullUrl = "https://example.com/full",
                customAlias = "custom-alias"
            )))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val payload = testJson.decodeFromString<ShortenUrlResponse>(response.bodyAsText())
        assertTrue(payload.shortUrl.endsWith("/custom-alias"))
    }

    fun `invalid url returns 400`() = testApplication {
        configureUrlShortenerApp()

        val response = client.post("/shorten") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(ShortenUrlRequest(
                fullUrl = "invalid-url"
            )))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `generated alias persists to repository`() = testApplication {
        val repository = configureUrlShortenerApp()

        val response = client.post("/shorten") {
            contentType(ContentType.Application.Json)
            setBody(testJson.encodeToString(ShortenUrlRequest(
                fullUrl = "https://example.com/auto"
            )))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val payload = testJson.decodeFromString<ShortenUrlResponse>(response.bodyAsText())
        assertTrue(payload.shortUrl.endsWith("/alias-0001"))

        val lookup = runBlocking { repository.readAlias("alias-0001") }
        require(lookup is ReadResult.Found)
        assertEquals("https://example.com/auto", lookup.value)
    }
}
