package com.tuule.eden.pipeline

import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.networking.entity
import com.tuule.eden.networking.request.RequestError
import com.tuule.eden.resource.Entity
import com.tuule.eden.util.asFailureResponse
import com.tuule.eden.util.decodeToString

internal class ContentTypeMatchTransformer(val delegate: ResponseTransformer,
                                           contentTypes: List<String>) : ResponseTransformer {

    override val description = "Content type match transformer for $contentTypes"

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

class ResponseContentTransformer<In : Any, Out : Any>(private val mismatchAction: InputTypeMismatchAction = InputTypeMismatchAction.ERROR,
                                                      private val processingErrors: Boolean = false,
                                                      private val processor: (Entity<In>) -> Out?) : ResponseTransformer {
    override val description = "Content type transformer "

    enum class InputTypeMismatchAction {
        ERROR,
        SKIP,
        SKIP_IF_OUTPUT_TYPE_MATCHES
    }

    private fun process(entity: Entity<Any>): Entity<Out>? =
            entity.retype<In>()
                    ?.let(processor)
                    ?.let { transformed -> entity.withEntity { transformed } }


    override fun transform(edenResponse: EdenResponse): EdenResponse = when (edenResponse) {
        is EdenResponse.Success -> processSuccess(edenResponse)
        is EdenResponse.Failure -> processError(edenResponse.error)
    }

    private fun processSuccess(success: EdenResponse.Success): EdenResponse =
            process(success.entity)?.let(EdenResponse::Success)
                    ?: contentTypeMismatchError(success.entity)

    private fun contentTypeMismatchError(entity: Entity<Any>) =
            when (mismatchAction) {
                InputTypeMismatchAction.SKIP,
                InputTypeMismatchAction.SKIP_IF_OUTPUT_TYPE_MATCHES -> EdenResponse.Success(entity)

                InputTypeMismatchAction.ERROR -> RequestError.Cause.WrongInputTypeInTransformerPipeline().asFailureResponse()
            }

    private fun processError(requestError: RequestError) =
            requestError.entity.takeIf { processingErrors }
                    ?.let(this::process)
                    ?.let { EdenResponse.Failure(RequestError(requestError.message, requestError.cause, it)) }
                    ?: EdenResponse.Failure(requestError)
}

fun createTextDecodingTransformer(processErrors: Boolean = true) =
        ResponseContentTransformer(processingErrors = processErrors, processor = Entity<ByteArray>::decodeToString)

