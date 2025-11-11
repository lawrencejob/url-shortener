package com.lawrencejob.service

import java.util.Base64

class EncoderService {
    fun encode(input: String): String {
        return Base64.getEncoder().encodeToString(input.toByteArray())
    }

    fun decode(input: String): String {
        return String(Base64.getDecoder().decode(input))
    }
}