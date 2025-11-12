package com.lawrencejob.unit

import com.lawrencejob.persistence.RedisService
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class RedisServiceTest {

    @Test 
    fun `Redis service refuses to accept invalid aliases`() = runBlocking {
        val redisService = RedisService(MockRedisAsyncCommands())

        val invalidAliases = listOf(
            "alias with spaces",
            "alias/with/slash",
            "alias\\with\\backslash",
            "alias?with?question",
            "alias#with#hash",
            "alias%with%percent",
            "alias@with@at",
            "alias!with!exclaim",
            "alias\$with\$dollar",
            "alias^with^caret",
            "alias&with&ampersand",
            "alias*with*asterisk",
            "alias(with(parentheses)",
            "alias)with)parentheses",
        )

        for (alias in invalidAliases) {
            val writeResult = redisService.writeAliasIfNotExists(alias, "http://example.com")
            assertTrue(writeResult is com.lawrencejob.persistence.WriteResult.Error, "Expected error for alias: $alias")

            val readResult = redisService.readAlias(alias)
            assertTrue(readResult is com.lawrencejob.persistence.ReadResult.Error, "Expected error for alias: $alias")

            val deleteResult = redisService.deleteAlias(alias)
            assertTrue(deleteResult is com.lawrencejob.persistence.DeleteResult.Error, "Expected error for alias: $alias")
        }
    }

}
