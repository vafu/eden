package com.tuule.eden.pipeline

import com.google.gson.JsonObject
import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.networking.asSuccess
import com.tuule.eden.networking.entity
import com.tuule.eden.networking.request.RequestError
import com.tuule.eden.networking.request.asFailure
import com.tuule.eden.resource.Entity
import java.nio.charset.Charset

internal class ContentTypeMatchTransformer(val delegate: ResponseTransformer,
                                           contentTypes: List<String>) : ResponseTransformer {
    override fun transform(edenResponse: EdenResponse): EdenResponse =
            edenResponse.entity?.contentType
                    ?.takeIf(contentTypeMatcher::matches)
                    ?.let { delegate.transform(edenResponse) } ?: edenResponse

    private val contentTypeMatcher: Regex

    init {
        contentTypeMatcher = Regex("^" + contentTypes.map {
            Regex.escape(it)
                    .replace("\\*", "[^/+]+")
        }.reduce { acc, s -> "$acc|$s" } + "($|;)")
    }
}

internal class TextDecodingTransformer : ResponseTransformer {
    override fun transform(edenResponse: EdenResponse): EdenResponse =
            edenResponse.asSuccess()
                    ?.entity
                    ?.retype<ByteArray>()
                    ?.withEntity { it.content.toString(it.charset.asCharset()) }
                    ?.let { EdenResponse.Success(it) }
                    ?: RequestError.Cause.DecodeError().asFailure(edenResponse.entity)

    private fun String?.asCharset() =
            try {
                Charset.forName(this)
            } catch (e: Exception) {
                Charsets.UTF_8
            }
}

class ResponseContentTransformer<In : Any, Out : Any>(private val mismatchAction: InputTypeMismatchAction = InputTypeMismatchAction.ERROR,
                                                      private val processingErrors: Boolean = false,
                                                      private val processor: (Entity<In>) -> Out?) : ResponseTransformer {
    enum class InputTypeMismatchAction {
        ERROR,
        SKIP,
        SKIP_IF_OUTPUT_TYPE_MATCHES
    }


    override fun transform(edenResponse: EdenResponse): EdenResponse = when (edenResponse) {
        is EdenResponse.Success -> processEntity(edenResponse.entity)
        is EdenResponse.Failure -> processError(edenResponse.error)
    }

    private fun processEntity(entity: Entity<Any>): EdenResponse =
            entity.retype<In>()
                    ?.let(processor)
                    ?.let { transformedData -> EdenResponse.Success(entity.withEntity { transformedData }) }
                    ?: contentTypeMismatchError(entity)

    private fun contentTypeMismatchError(entity: Entity<Any>) =
            when (mismatchAction) {
                InputTypeMismatchAction.SKIP,
                InputTypeMismatchAction.SKIP_IF_OUTPUT_TYPE_MATCHES -> EdenResponse.Success(entity)

                InputTypeMismatchAction.ERROR -> RequestError.Cause.WrongInputTypeInTransformerPipeline().asFailure()
            }

    private fun processError(requestError: RequestError) =
            requestError.entity.takeIf { processingErrors }
                    ?.let(this::processEntity)
                    ?.takeIf { it is EdenResponse.Success }
                    ?: EdenResponse.Failure(requestError)
}