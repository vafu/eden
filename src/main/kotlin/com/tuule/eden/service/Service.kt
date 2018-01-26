package com.tuule.eden.service

import com.tuule.eden.multiplatform.*
import com.tuule.eden.networking.NetworkingProvider
import com.tuule.eden.resource.Resource

open class Service(baseUrl: String,
                   internal val networkingProvider: NetworkingProvider) {

    //todo multiplatform
    private val cache: WeakCache<String, Resource<*>> = WeakCacheJVM()

    val baseUrl: String? = baseUrl.asURL()

    fun <T> resourceFromAbsoluteURL(absoluteURL: String): Resource<T> =
            absoluteURL.asURL()?.let {
                (cache.getOrPut(it) { Resource<T>(this, it) } as? Resource<T>)
                        ?: throw Exception("Already have a resource with different content type")
            } ?: throw Exception("Bad resource url $absoluteURL")

    fun <T> resource(baseUrl: String?, path: String) = resourceFromAbsoluteURL<T>("$baseUrl/${path.removePrefix("/")}")

    fun <T> resource(path: String): Resource<T> = resource(baseUrl, path)


}