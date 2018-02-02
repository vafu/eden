package com.tuule.eden.networking

import com.tuule.eden.multiplatform.now
import com.tuule.eden.resource.Entity

class RequestError(message: String,
                   cause: Cause?,
                   val entity: Entity<Any>? = null) : Throwable(message, cause) {

    var httpStatusCode: Int? = null

    val timestamp = now()

    constructor(response: Response,
                content: Any?,
                cause: Cause?,
                userMessage: String? = null) : this(
            userMessage ?: cause?.localizedMessage ?: response.message ?: "Request failed for unknown reason",
            cause,
            content?.let { Entity(response, content) }) {

        httpStatusCode = response.code
    }

    sealed class Cause(message: String) : Throwable(message) {
        class RequestCanceled : Cause("Request has been canceled")
    }
}

