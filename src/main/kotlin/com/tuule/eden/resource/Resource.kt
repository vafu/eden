package com.tuule.eden.resource

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.networking.request.*
import com.tuule.eden.resource.configuration.RequestMutation
import com.tuule.eden.service.ResourceService
import com.tuule.eden.util.addPath
import com.tuule.eden.util.mutate
import kotlinx.coroutines.experimental.async

open class Resource<T : Any>(val service: ResourceService,
                             val url: String) {
    //<editor-fold desc="data">
    var data: Entity<T>? = null

    //</editor-fold>

    //<editor-fold desc="configuration">

    internal val configuration by lazy { service.configuration(this, RequestMethod.GET) }

    internal fun configuration(method: RequestMethod) =
            service.configuration(this, method)

    internal fun configuration(request: HTTPRequest) =
            service.configuration(this, request.method)

    //</editor-fold>


    //<editor-fold desc="requesting">
    private val getRequests = mutableSetOf<Request>()
    private val allRequests = mutableSetOf<Request>()

    val isRequesting
        get() = allRequests.isNotEmpty()

    val isLoading
        get() = getRequests.isNotEmpty()


    fun request(method: RequestMethod, mutate: RequestMutation = { it }): TypedRequest<T> {
        return TypedRequest<T>(this) {
            val configuration = configuration(method)
            configuration.requestMutations
                    .mutate(HTTPRequest(url, method, configuration.headers))
                    .let(mutate)
        }
                .also { trackRequest(it, allRequests) }
                .also { it.start() }
    }

    fun load() =
            request(RequestMethod.GET)


    private fun trackRequest(request: Request, set: MutableSet<Request>) {
        set.add(request)
        request.onCompletion {
            allRequests.removeIf(Request::isCompleted)
            getRequests.removeIf(Request::isCompleted)
        }
    }
    //</editor-fold>

    //<editor-fold desc="cache">

    private fun getCachedEntityAsync() = async {
        (configuration.cache as? EntityCache<T>)
                ?.let { it[this@Resource] }
    }

    internal fun cacheEntityAsync(entity: Entity<T>) = async {
        (configuration.cache as? EntityCache<T>)
                ?.let {
                    debugLog(LogCategory.CACHE, "caching $entity")
                    it[this@Resource] = entity
                }
    }
    //</editor-fold>

    fun <T : Any> child(path: String) = service.resourceFromAbsoluteURL<T>(url.addPath(path))


    override fun toString() = url
}

