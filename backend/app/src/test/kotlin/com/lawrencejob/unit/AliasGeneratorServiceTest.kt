package com.lawrencejob.unit

import com.lawrencejob.service.AliasGeneratorService
import kotlin.test.Test
import kotlin.test.assertTrue

class AliasGeneratorServiceTest {

    @Test 
    fun `AliasGeneratorService generates unique aliases`() {
        val service = AliasGeneratorService()
        val alias1 = service.generateAlias()
        val alias2 = service.generateAlias()
        
        assertTrue(alias1.isNotEmpty())
        assertTrue(alias2.isNotEmpty())
        assertTrue(alias1 != alias2) 
    }

}