package com.tuule.eden.networking

import com.tuule.eden.resource.Entity

class Request(val url: String,
              val method: RequestMethod = RequestMethod.GET,
              val headers: Map<String, String> = emptyMap(),
              val body: ByteArray? = null) {

    override fun toString() = "<$method>: $url" + if (headers.isNotEmpty()) "\nHeaders: $headers" else ""
}

enum class RequestMethod {
    GET,
    HEAD,
    POST,
    PUT,
    PATCH,
    DELETE
}

//interface EdenRequest {
//    fun onCompleted(callback : (ResponseInfo) -> Unit) : EdenRequest
//    fun <T> onSuccess(callback: (Entity<T>) -> Unit): EdenRequest
//    fun <T> onNewData
//}
interface NetworkCallback<T> {
    fun onSuccess(callback: (Entity<T>) -> Unit): NetworkCallback<T>
    fun onError(callback: (RequestError) -> Unit): NetworkCallback<T>
    fun cancel()
}

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