package com.tuule.eden.service

import com.tuule.eden.multiplatform.*
import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.networking.NetworkingProvider
import com.tuule.eden.networking.asSuccess
import com.tuule.eden.networking.request.RequestError
import com.tuule.eden.pipeline.Pipeline
import com.tuule.eden.pipeline.getTransformer
import com.tuule.eden.resource.Resource
import com.tuule.eden.resource.configuration.*
import com.tuule.eden.util.addPath
import com.tuule.eden.util.asValidUrl
import java.nio.charset.Charset

open class ResourceService(baseUrl: String? = null,
                           internal val networkingProvider: NetworkingProvider) {

    open val configBuilder =
            configuration { resource ->

                pipeline {

                    get(Pipeline.StageKey.MODEL).add(resource.getTransformer())
                }
            }


    //todo multiplatform
    private val cache: WeakCache<String, Resource<*>> = WeakCacheJVM()

    private val baseUrl: String? = baseUrl?.asValidUrl()

//<editor-fold desc="resource creation">

    fun <T : Any> resourceFromAbsoluteURL(absoluteURL: String): Resource<T> =
            absoluteURL.asValidUrl().let {
                (cache.getOrPut(it) { Resource<T>(this, it) } as? Resource<T>)
                        ?: throw RuntimeException("Already have a resource with different content type")
            }

    fun <T : Any> resource(baseUrl: String, path: String) =
            resourceFromAbsoluteURL<T>(baseUrl.addPath(path))

    fun <T : Any> resource(path: String): Resource<T> =
            baseUrl?.let { resource<T>(it, path) }
                    ?: throw RuntimeException("Cannot create resource from path. Service does not have base url")

//</editor-fold>

}

