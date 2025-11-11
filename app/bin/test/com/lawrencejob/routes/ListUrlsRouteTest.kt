package com.lawrencejob.routes

import com.lawrencejob.util.InMemoryShortUrlRepository
import com.lawrencejob.util.configureUrlShortenerApp
import com.lawrencejob.util.testJson
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ListUrlsRouteTest {

    @Test
    fun `GET urls streams stored aliases`() = testApplication {
        val repository = InMemoryShortUrlRepository().also {
            runBlocking {
                it.writeAliasIfNotExists("one", "https://example.com/1")
                it.writeAliasIfNotExists("two", "https://example.com/2")
            }
        }

        configureUrlShortenerApp(repository = repository)

        val response = client.get("/urls")
        assertEquals(HttpStatusCode.OK, response.status)

        val urls = testJson.parseToJsonElement(response.bodyAsText()).jsonArray
        assertEquals(2, urls.size)
        assertEquals("one", urls[0].jsonObject["alias"]?.jsonPrimitive?.content)
        assertEquals("two", urls[1].jsonObject["alias"]?.jsonPrimitive?.content)
    }
}
