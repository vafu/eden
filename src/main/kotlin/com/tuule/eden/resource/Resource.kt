package com.tuule.eden.resource

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.multiplatform.now
import com.tuule.eden.networking.request.*
import com.tuule.eden.resource.configuration.Configuration
import com.tuule.eden.resource.configuration.RequestMutation
import com.tuule.eden.service.ResourceService
import com.tuule.eden.util.addPath
import com.tuule.eden.util.debugLogWithValue
import com.tuule.eden.util.mutate
import kotlinx.coroutines.experimental.async
import java.lang.reflect.Type
import kotlin.math.max

open class Resource<T : Any>(val service: ResourceService,
                             val url: String,
                             val dataType: Type) {

    //<editor-fold desc="configuration">

    internal val configuration
        get() = configuration(RequestMethod.GET)

    private val configurationCache = mutableMapOf<RequestMethod, Configuration>()

    private fun configuration(method: RequestMethod): Configuration =
            configurationCache[method]
                    ?.takeIf { isConfigurationValid }
                    ?: service.configuration(this, method)
                    .also {
                        configurationCache[method] = it
                        configVersion = service.configVersion
                    }

    private var configVersion = 0L

    private val isConfigurationValid
        get() = service.configVersion == configVersion


    //</editor-fold>

    init {
        loadDataFromCacheAsync()
    }

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

    private val entityCache
        get() = (configuration.cache as? EntityCache<T>)

    private fun getCachedEntityAsync() = async { entityCache?.get(this@Resource) }

    private fun removeCachedEntityAsync() = async { entityCache?.remove(this@Resource) }

    internal fun cacheEntityAsync(entity: Entity<T>) = async {
        entityCache?.let {
            debugLog(LogCategory.CACHE, "caching $entity")
            it[this@Resource] = entity
        }
    }

    private fun touchCacheEntriesAsync(timestamp: Long) = async {
        entityCache?.touch(timestamp, this@Resource)
    }

    private fun loadDataFromCacheAsync() = async {
        getCachedEntityAsync().await()
                ?.let { processNewData(it, DataSource.CACHE) }
                ?: debugLogWithValue(LogCategory.CACHE, "skipping cache load for ${this@Resource}") { null }
    }
    //</editor-fold>

    fun <R : Any> child(path: String, type: Type) =
            service.resourceFromAbsoluteURL<R>(url.addPath(path), type)


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

inline fun <reified R : Any> Resource<*>.child(path: String) =
        service.resourceFromAbsoluteURL<R>(url.addPath(path), R::class.java)

private val Resource<*>.isUpToDate
    get() = !invalidated && (now() - lastChanged <= retryTime)

private val Resource<*>.lastChanged
    get() = max(data?.timestamp ?: 0L, latestError?.timestamp ?: 0L)

private val Resource<*>.retryTime
    get() = latestError?.let { configuration.retryTime } ?: configuration.expirationTime