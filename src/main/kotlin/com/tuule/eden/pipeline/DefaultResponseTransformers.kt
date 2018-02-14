package com.tuule.eden.pipeline

import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.networking.asSuccess
import com.tuule.eden.networking.request.RequestError
import java.nio.charset.Charset

class StringDecodingTransformer : ResponseTransformer {
    override fun transform(edenResponse: EdenResponse): EdenResponse =
            edenResponse.asSuccess()
                    ?.entity
                    ?.retype<ByteArray>()
                    ?.map { it.content.toString(Charset.forName(it.charset)) }
                    ?.let { EdenResponse.Success(it) }
                    ?: EdenResponse.Failure(RequestError.Cause.DecodeError())

}