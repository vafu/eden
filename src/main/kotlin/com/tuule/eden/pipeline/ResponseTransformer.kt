package com.tuule.eden.pipeline

import com.tuule.eden.networking.EdenResponse

interface ResponseTransformer {
    fun transform(edenResponse: EdenResponse): EdenResponse
}