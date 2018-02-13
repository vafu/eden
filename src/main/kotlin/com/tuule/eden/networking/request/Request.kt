package com.tuule.eden.networking.request

import com.tuule.eden.networking.RequestInFlight
import com.tuule.eden.networking.ResponseInfo
import com.tuule.eden.resource.Entity
import com.tuule.eden.resource.Resource
import kotlinx.coroutines.experimental.async

class HTTPRequest(val url: String,
                  val method: RequestMethod = RequestMethod.GET,
                  val headers: Map<String, String> = emptyMap(),
                  val body: ByteArray? = null)

enum class RequestMethod {
    GET,
    HEAD,
    POST,
    PUT,
    PATCH,
    DELETE
}

internal interface RequestBuilder {
    fun onSuccess(callback: (Entity<Any>) -> Unit)
    fun onFailure(callback: () -> Unit)
    fun onCompletion(callback: (ResponseInfo) -> Unit)
    fun onNewData(callback: (Entity<Any>) -> Unit)
    fun onNotModified(callback: () -> Unit)

    fun repeated(): RequestBuilder?
    fun cancel()
    fun start()
}