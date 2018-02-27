package com.tuule.eden.resource

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.multiplatform.now
import com.tuule.eden.networking.request.*
import com.tuule.eden.resource.configuration.RequestMutation
import com.tuule.eden.service.ResourceService
import com.tuule.eden.util.addPath
import com.tuule.eden.util.log
import com.tuule.eden.util.mutate
import kotlinx.coroutines.experimental.async
import kotlin.math.max

open class Resource<T : Any>(val service: ResourceService,
                             val url: String) {

    //<editor-fold desc="configuration">

    internal val configuration by lazy { service.configuration(this, RequestMethod.GET) }

    internal fun configuration(method: RequestMethod) =
            service.configuration(this, method)

    internal fun configuration(request: HTTPRequest) =
            service.configuration(this, request.method)

    //</editor-fold>

    //<editor-fold desc="data">
    var invalidated = false
        private set

    var data: Entity<T>? = null
        private set

    var latestError: RequestError? = null
        private set

    //</editor-fold>

    //<editor-fold desc="requesting">
    private val getRequests = mutableSetOf<TypedRequest<T>>()
    private val allRequests = mutableSetOf<TypedRequest<Any>>()

    fun <R : Any> request(method: RequestMethod, mutate: RequestMutation = { it }): TypedRequest<R> {
        return TypedRequest<R>(this) {
            val configuration = configuration(method)
            configuration.requestMutations
                    .mutate(HTTPRequest(url, method, configuration.headers))
                    .let(mutate)
        }
                .also { trackRequest(it, allRequests) }
                .also { it.start() }
    }

    fun load(request: TypedRequest<T> = request(RequestMethod.GET)): TypedRequest<T> {
        trackRequest(request, getRequests)

        request
                .onNewData { processNewData(it) }
                .onNotModified(::processNotModified)
                .onFailure(::processError)

        return request
    }

    fun loadIfNeeded() =
            getRequests.firstOrNull() ?:
                    takeUnless { isUpToDate }?.let { load() }


    private fun <R : Any> trackRequest(request: TypedRequest<R>, set: MutableSet<TypedRequest<R>>) {
        set.add(request)
        request.onCompletion {
            set.removeIf(Request::isCompleted)
        }
    }

    private fun processNewData(entity: Entity<Any>, source: DataSource = DataSource.NETWORK) {
        debugLog(LogCategory.STATE_CHANGES, "received new data from $source : $entity")
        latestError = null
        entity.retype<T>()
                ?.let { data = it }
                ?: let {
            processError(RequestError("Can't retype response $entity to resource's type", entity = entity))
            return
        }

        if (source == DataSource.OVERRIDE)
            removeCachedEntityAsync()
    }

    private fun processNotModified() {
        debugLog(LogCategory.STATE_CHANGES, "data is still valid")

        latestError = null
        data?.touch()?.let { touchCacheEntriesAsync(it.timestamp) }


    }

    private fun processError(requestError: RequestError) {
        latestError = requestError
    }

    //</editor-fold>

    //<editor-fold desc="cache">

    private fun getCachedEntityAsync() = async {
        (configuration.cache as? EntityCache<T>)
                ?.let { it[this@Resource] }
    }

    private fun removeCachedEntityAsync() = async {
        (configuration.cache as? EntityCache<T>)
                ?.let { it.remove(this@Resource) }
    }

    internal fun cacheEntityAsync(entity: Entity<T>) = async {
        (configuration.cache as? EntityCache<T>)
                ?.let {
                    debugLog(LogCategory.CACHE, "caching $entity")
                    it[this@Resource] = entity
                }
    }

    private fun touchCacheEntriesAsync(timestamp: Long) = async {
        (configuration.cache as? EntityCache<T>)
                ?.let { it.touch(timestamp, this@Resource) }
    }
    //</editor-fold>

    fun <T : Any> child(path: String) = service.resourceFromAbsoluteURL<T>(url.addPath(path))


    override fun toString() = url

    enum class DataSource {
        NETWORK,
        CACHE,
        OVERRIDE,
        WIPE;

        override fun toString() =
                super.toString().toLowerCase()
    }
}

internal

val Resource<*>.isUpToDate
    get() = !invalidated && (now() - lastChanged <= retryTime)

private val Resource<*>.lastChanged
    get() = max(data?.timestamp ?: 0L, latestError?.timestamp ?: 0L)

private val Resource<*>.retryTime
    get() = latestError?.let { configuration.retryTime } ?: configuration.expirationTime