package com.tuule.eden.service

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.WeakCache
import com.tuule.eden.multiplatform.WeakCacheJVM
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.networking.NetworkingProvider
import com.tuule.eden.networking.request.RequestMethod
import com.tuule.eden.pipeline.Pipeline
import com.tuule.eden.pipeline.createTextDecodingTransformer
import com.tuule.eden.pipeline.getJsonTransformer
import com.tuule.eden.resource.Resource
import com.tuule.eden.resource.configuration.Configuration
import com.tuule.eden.resource.configuration.ConfigurationEntry
import com.tuule.eden.resource.configuration.ConfigurationMutator
import com.tuule.eden.util.addPath
import com.tuule.eden.util.asValidUrl
import java.lang.reflect.Type

open class ResourceService(baseUrl: String? = null,
                           internal val networkingProvider: NetworkingProvider) {


    //todo multiplatform
    private val cache: WeakCache<String, Resource<*>> = WeakCacheJVM()

    private val baseUrl: String? = baseUrl?.asValidUrl()

    //<editor-fold desc="configuration">
    private val configEntries = mutableListOf<ConfigurationEntry>()

    init {
        configure { resource ->
            copy(pipeline = pipeline.apply {
                get(Pipeline.StageKey.DECODING).add(createTextDecodingTransformer())
                get(Pipeline.StageKey.MODEL).add(resource.getJsonTransformer())
            })
        }
    }

    internal var configVersion = 0L

    fun invalidateConfigs() {
        debugLog(LogCategory.CONFIGURATION, "Invalidating configs")
        configVersion += 1
    }

    internal fun configuration(resource: Resource<*>, requestMethod: RequestMethod): Configuration {
        debugLog(LogCategory.CONFIGURATION, "Creating configuration for $requestMethod of $resource")
        return configEntries
                .filter { it.methods.contains(requestMethod) }
                .filter { it.urlMatcher(resource.url) }
                .fold(Configuration()) { acc, value ->
                    debugLog(LogCategory.CONFIGURATION, " ├╴ Applying $value")
                    value.configurator(acc, resource)
                }
                .also { debugLog(LogCategory.CONFIGURATION, " └╴ Final configuration: $it") }
    }

    fun configure(urlMatcher: (String) -> Boolean = { true },
                  requestMethods: Array<RequestMethod> = RequestMethod.values(),
                  description: String? = null,
                  configurer: ConfigurationMutator) {
        configEntries.add(ConfigurationEntry(
                requestMethods.toSet(),
                urlMatcher,
                description,
                configurer)
                .also { debugLog(LogCategory.CONFIGURATION, "Added $it") })
    }

    //</editor-fold>

    //<editor-fold desc="resource creation">

    fun <T : Any> resourceFromAbsoluteURL(absoluteURL: String, type: Type): Resource<T> =
            absoluteURL.asValidUrl().let {
                println(type.typeName)
                (cache.getOrPut(it) { Resource<T>(this, it, type) } as? Resource<T>)
                        ?: throw RuntimeException("Already have a resource with different content type")
            }

    fun <T : Any> resource(baseUrl: String, path: String, type: Type) =
            resourceFromAbsoluteURL<T>(baseUrl.addPath(path), type)

    fun <T : Any> resource(path: String, type: Type): Resource<T> =
            baseUrl?.let { resource<T>(it, path, type) }
                    ?: throw RuntimeException("Cannot create resource from path. Service does not have base url")

    //</editor-fold>

}

inline fun <reified T : Any> ResourceService.resource(path: String) = resource<T>(path, T::class.java)
inline fun <reified T : Any> ResourceService.resource(baseUrl: String, path: String) = resource<T>(baseUrl, path, T::class.java)
inline fun <reified T : Any> ResourceService.resourceFromAbsoluteUrl(absoluteURL: String) = resourceFromAbsoluteURL<T>(absoluteURL, T::class.java)