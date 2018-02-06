package com.tuule.eden.networking

import com.tuule.eden.networking.request.RequestError
import com.tuule.eden.resource.Entity

class HTTPResponse(val body: ByteArray?,
                   val code: Int,
                   val headers: Map<String, String>,
                   val message: String?)

sealed class EdenResponse {
    class Success(val entity: Entity<Any>) : EdenResponse()
    class Failure(val error: Throwable) : EdenResponse()

    val cancellation
        get() = (this as Failure).error is RequestError.Cause.RequestCanceled
}

data class ResponseInfo(val response: EdenResponse, val isNew: Boolean = true) {
    companion object {
        internal val cancelation = ResponseInfo(EdenResponse.Failure(RequestError.Cause.RequestCanceled()))
    }
}