package com.lawrencejob.api

import kotlinx.serialization.Serializable

// schema:
//     type: array
//     items:
//         type: object
//         properties:
//         alias:
//             type: string
//             example: my-custom-alias
//         fullUrl:
//             type: string
//             example: https://example.com/very/long/url
//         shortUrl:
//             type: string
//             example: http://localhost:8080/my-custom-alias

@Serializable
data class ListAllUrlsResponseItem(
    val alias: String,
    val fullUrl: String,
    val shortUrl: String
)