package com.tuule.eden.resource

import com.tuule.eden.networking.Request

class Configuration(val expirationTime: Long = 30000,
                    val retryTime: Long = 1000,
                    val headers: Map<String, String> = emptyMap())


typealias RequestMutation = (Request) -> Unit
typealias RequestDecoration<T> = (Request, Resource<T>) -> Unit