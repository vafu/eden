package com.tuule.eden.util

import com.tuule.eden.networking.HTTPResponse
import com.tuule.eden.resource.Entity

internal val HTTPResponse.isError
    get() = code >= 400

fun HTTPResponse.toByteArrayEntity() =
        Entity(this, body ?: byteArrayOf())
