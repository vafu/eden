package com.tuule.eden.resource

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.networking.Request
import com.tuule.eden.networking.RequestInFlight
import com.tuule.eden.networking.RequestMethod
import com.tuule.eden.service.ResourceService
import com.tuule.eden.util.addPath

class Resource<T>(val service: ResourceService,
                  val url: String) {

    var data: Entity<String>? = null

    fun <T> child(path: String) = service.resourceFromAbsoluteURL<T>(url.addPath(path))

    fun load(): RequestInFlight {
        val underlyingRequest = Request(url, RequestMethod.GET)
        debugLog(LogCategory.NETWORK_DETAILS, "performing $underlyingRequest")

        return service.networkingProvider.performRequest(underlyingRequest) { data, error ->
            this@Resource.data = data?.let {
                Entity(it, data.body!!).map { entity ->
                    entity.content.toString(Charsets.ISO_8859_1)
                }
            }
            debugLog(LogCategory.NETWORK_DETAILS, "resulted with: ${this@Resource.data?.content}")
        }
    }
}