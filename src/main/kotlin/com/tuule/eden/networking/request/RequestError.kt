package com.tuule.eden.networking.request

import com.tuule.eden.multiplatform.now
import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.networking.HTTPResponse
import com.tuule.eden.resource.Entity

class RequestError(message: String? = null,
                   cause: Cause? = null,
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
        class TypeMismatchError(response: EdenResponse) : Cause("Type mismatch while transforming $response")
        class DecodeError : Cause("Cannot decode given data")
        class WrongInputTypeInTransformerPipeline : Cause("Wrong Input Type In Transformer Pipeline")
        class User(throwable: Throwable) : Cause(throwable.localizedMessage)
    }
}


internal fun RequestError.Cause.asFailure(entity: Entity<Any>? = null) =
        EdenResponse.Failure(RequestError(message, this, entity))

internal fun Throwable.asFailure(entity: Entity<Any>? = null) =
        EdenResponse.Failure(RequestError(message, RequestError.Cause.User(this), entity))

