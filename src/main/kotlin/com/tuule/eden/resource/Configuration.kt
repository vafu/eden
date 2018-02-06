package com.tuule.eden.resource

import com.tuule.eden.networking.request.HTTPRequest

class Configuration(val expirationTime: Long = 30000,
                    val retryTime: Long = 1000,
                    val headers: Map<String, String> = emptyMap())

typealias RequestMutation = (HTTPRequest) -> Unit
typealias RequestDecoration<T> = (HTTPRequest, Resource<T>) -> Unit