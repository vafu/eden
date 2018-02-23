package com.tuule.eden.networking

import com.tuule.eden.networking.request.RequestError
import com.tuule.eden.networking.request.asFailure
import com.tuule.eden.resource.Entity

class HTTPResponse(val body: ByteArray?,
                   val code: Int,
                   val headers: Map<String, String>,
                   val message: String?)

fun HTTPResponse.toByteArrayEntity() =
        Entity(this, body ?: byteArrayOf())

sealed class EdenResponse {
    class Success(val entity: Entity<Any>) : EdenResponse() {
        override fun toString() = "Success with $entity"
    }

    class Failure(val error: RequestError) : EdenResponse() {
        override fun toString() = "Failed with ${error.message}"
    }


    val cancellation
        get() = (this as? Failure)?.error?.cause is RequestError.Cause.RequestCanceled

}

data class ResponseInfo(val response: EdenResponse, val isNew: Boolean = true) {
    companion object {
        internal val cancelation = ResponseInfo(RequestError.Cause.RequestCanceled().asFailure())
    }
}

val EdenResponse.entity: Entity<Any>?
    get() = when (this) {
        is EdenResponse.Success -> this.entity
        is EdenResponse.Failure -> error.entity
    }


fun EdenResponse.asSuccess() = this as? EdenResponse.Success