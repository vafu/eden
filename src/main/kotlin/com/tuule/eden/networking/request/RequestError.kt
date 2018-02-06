package com.tuule.eden.networking.request

import com.tuule.eden.multiplatform.now
import com.tuule.eden.networking.HTTPResponse
import com.tuule.eden.resource.Entity

class RequestError(message: String,
                   cause: Cause?,
                   val entity: Entity<Any>? = null) : Throwable(message, cause) {

    var httpStatusCode: Int? = null

    val timestamp = now()

    constructor(httpResponse: HTTPResponse,
                content: Any? = null,
                cause: Cause? = null,
                userMessage: String? = null) : this(
            userMessage ?: cause?.localizedMessage ?: httpResponse.message ?: "HTTPRequest failed for unknown reason",
            cause,
            content?.let { Entity(httpResponse, content) }) {

        httpStatusCode = httpResponse.code
    }

    sealed class Cause(message: String) : Throwable(message) {
        class RequestCanceled : Cause("HTTPRequest has been canceled")
        class NoLocalDataForNotModified : Cause("No local data available when got 304")

    }
}

