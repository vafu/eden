package com.tuule.eden.resource.configuration

import com.tuule.eden.networking.request.HTTPRequest
import com.tuule.eden.pipeline.Pipeline
import com.tuule.eden.resource.EntityCache

data class ResourceConfiguration(val expirationTime: Long = 30 * 1000,
                                 val retryTime: Long = 1 * 1000,
                                 val headers: Map<String, String> = emptyMap(),
                                 val requestMutations: List<RequestMutation> = emptyList(),
                                 val pipeline: Pipeline = Pipeline(),
                                 val cache: EntityCache<*>? = null)


typealias RequestMutation = (HTTPRequest) -> HTTPRequest

fun ResourceConfiguration.addHeader(key: String, value: String) =
        copy(headers = headers.toMutableMap().apply { set(key, value) })