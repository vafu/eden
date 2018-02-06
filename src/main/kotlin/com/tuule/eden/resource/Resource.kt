package com.tuule.eden.resource

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.networking.request.HTTPRequest
import com.tuule.eden.networking.RequestInFlight
import com.tuule.eden.networking.request.NetworkRequestBuilder
import com.tuule.eden.networking.request.RequestMethod
import com.tuule.eden.service.ResourceService
import com.tuule.eden.util.addPath

class Resource<T : Any>(val service: ResourceService,
                        val url: String) {

    var data: Entity<T>? = null

    fun <T> child(path: String) = service.resourceFromAbsoluteURL<T>(url.addPath(path))

    fun request() {
        NetworkRequestBuilder(this) {
            HTTPRequest(url)
        }.apply {
            onCompletion { debugLog(LogCategory.NETWORK, "test") }
        }
                .start()
    }
}