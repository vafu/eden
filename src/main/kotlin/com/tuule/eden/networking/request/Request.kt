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

typealias RequestMutation = (HTTPRequest) -> Unit
typealias RequestDecoration<T> = (HTTPRequest, Resource<T>) -> Unit
//    fun addMutation(mutation: RequestMutation)
//    fun addDecoration(decoration: RequestDecoration<Any>)


internal interface RequestBuilder {
    fun onSuccess(callback: (Entity<Any>) -> Unit)
    fun onFailure(callback: () -> Unit)
    fun onCompletion(callback: (ResponseInfo) -> Unit)
    fun onNewData(callback: (Entity<Any>) -> Unit)
    fun onNotModified(callback: () -> Unit)

    fun repeated(): RequestBuilder?
    fun start(): RequestInFlight?
}

internal class Callbacks<T> {
    var result: T? = null
        private set

    private val callbacks = mutableSetOf<(T) -> Unit>()

    fun addCallback(callback: (T) -> Unit) {
        result?.let { async { callback(it) } } ?: callbacks.add(callback)
    }

    fun notify(value: T) {
        callbacks.forEach { it(value) }
    }

    fun notifyOfCompletion(value: T) {
        result = value
        async {
            notify(value)
        }.invokeOnCompletion { callbacks.clear() }
    }
}