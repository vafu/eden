package com.tuule.eden.pipeline

import com.google.gson.JsonElement
import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.resource.Resource

interface ResponseTransformer {
    fun transform(edenResponse: EdenResponse): EdenResponse
    val description: String
}

