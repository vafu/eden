package com.tuule.eden.resource

import com.tuule.eden.multiplatform.now
import com.tuule.eden.networking.Response

data class Entity<T>(val content: T,
                     private val _headers: Map<String, String>,
                     val timestamp: Long = now()) {

    constructor(data: T,
                contentType: String,
                headers: Map<String, String>) : this(data, headers.toMutableMap()
            .also { it["content-type"] = contentType })

    constructor(response: Response, data: T) : this(data, response.headers)


    var headers: Map<String, String> = _headers.normalizeKeys()
        private set(value) {
            field = value.normalizeKeys()
        }

    val charset: String? by lazy { Regex("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)").find(contentType)?.value }

    val contentType: String
        get() = headers["content-type"] ?: "application/octet-stream"

    val etag: String?
        get() = headers["etag"]

    inline fun <R> map(map: (Entity<T>) -> R) = map(this).let { Entity(it, headers, timestamp) }
    fun <R> retype() = (content as? R)?.let { Entity(it, headers, timestamp) }
}

private fun Map<String, String>.normalizeKeys() = mapKeys { it.key.toLowerCase() }

fun Map<String, String>.header(key: String) = normalizeKeys()[key.toLowerCase()]