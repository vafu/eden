package com.tuule.eden.networking.request

import com.tuule.eden.resource.Entity
import com.tuule.eden.resource.Resource

class TypedRequest<out T : Any>(resource: Resource<*>,
                                requestProducer: () -> HTTPRequest) :
        Request by NetworkRequest(resource, requestProducer) {

    fun onData(callback: (T) -> Unit) = onSuccess { e: Entity<Any> ->
        (e.content as? T)?.let(callback)
    }
}
