package com.lawrencejob.service

import io.viascom.nanoid.NanoId

class AliasGeneratorService {
    val alphabet: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun generateAlias(): String {
        // generate a random alias
        // a length of 16 equates to a 1% chance of collision in 1,000 years of 1,000 tokens per second
        return NanoId.generate(16, alphabet)
    }
}