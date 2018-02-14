package com.tuule.eden.resource

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.networking.request.HTTPRequest
import com.tuule.eden.networking.request.NetworkRequestBuilder
import com.tuule.eden.resource.configuration.configuration
import com.tuule.eden.service.ResourceService
import com.tuule.eden.util.addPath
import kotlinx.coroutines.experimental.async

open class Resource<T : Any>(val service: ResourceService,
                             val url: String) {

    protected open val config = configuration { }

    internal val _configuration by lazy {
        config.asSingleMutator().invoke(service.configBuilder(this))
    }

    var data: Entity<T>? = null

    fun <T : Any> child(path: String) = service.resourceFromAbsoluteURL<T>(url.addPath(path))

    internal fun request() =
            NetworkRequestBuilder(this) {
                HTTPRequest(url)
            }


    //<editor-fold desc="cache">

    private fun getCachedEntityAsync() = async {
        (_configuration.cache as? EntityCache<T>)
                ?.let { it[this@Resource] }
    }

    internal fun cacheEntityAsync(entity: Entity<T>) = async {
        (_configuration.cache as? EntityCache<T>)
                ?.let {
                    debugLog(LogCategory.CACHE, "caching $entity")
                    it[this@Resource] = entity
                }
    }
    //</editor-fold>

}

