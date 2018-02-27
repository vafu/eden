package com.tuule.eden.networking.request

import com.tuule.eden.networking.ResponseInfo
import com.tuule.eden.resource.Entity

data class HTTPRequest(val url: String,
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

interface Request {
    fun onSuccess(callback: (Entity<Any>) -> Unit): Request
    fun onFailure(callback: (RequestError) -> Unit): Request
    fun onCompletion(callback: (ResponseInfo) -> Unit): Request
    fun onNewData(callback: (Entity<Any>) -> Unit): Request
    fun onNotModified(callback: () -> Unit): Request

    val isCompleted: Boolean
    fun repeated(): Request?
    fun cancel()
    fun start(): Request
}