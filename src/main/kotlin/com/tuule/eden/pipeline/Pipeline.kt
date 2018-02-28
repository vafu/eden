package com.tuule.eden.pipeline

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.resource.Resource

class Pipeline {

    private val stages = mutableMapOf<StageKey, PipelineStage>()

    operator fun get(key: StageKey) = stages.getOrPut(key, { PipelineStage(key) })
    operator fun set(key: StageKey, value: PipelineStage) {
        stages[key] = value
    }

    fun clear() = stages.forEach { _, u -> u.clear() }

    enum class StageKey {
        RAW_DATA,
        DECODING,
        PARSING,
        MODEL,
        CLEANUP
    }

    override fun toString() = stages.toString()
}

class PipelineStage(private val key: Pipeline.StageKey) {
    private val transformers = mutableSetOf<ResponseTransformer>()

    fun add(responseTransformer: ResponseTransformer) {
        transformers.add(responseTransformer)
    }

    fun clear() {
        transformers.clear()
    }

    internal val isActive
        get() = transformers.isNotEmpty()

    internal fun process(response: EdenResponse) =
            transformers.fold(response) { acc, func -> func.transform(acc) }

    override fun toString() = key.name.toLowerCase()
}

inline fun PipelineStage.add(description: String? = null, crossinline transformation: (EdenResponse) -> EdenResponse) {
    add(object : ResponseTransformer {
        override val description: String
            get() = description ?: ""

        override fun transform(edenResponse: EdenResponse) =
                transformation(edenResponse)
    })
}


private val Pipeline.activeStages
    get() = Pipeline.StageKey.values()
            .map(this::get)
            .filter(PipelineStage::isActive)


internal fun <T : Any> Pipeline.processAndCache(response: EdenResponse, resource: Resource<T>) =
        activeStages
                .fold(response) { acc, stage ->
                    stage.process(acc)
                            .also { debugLog(LogCategory.PIPELINE, " ├╴ $it on stage $stage") }
                }
                .also { debugLog(LogCategory.PIPELINE, " └╴Response after pipeline: $it") }
                .apply(resource::cacheResponseAsync)

private fun <T : Any> Resource<T>.cacheResponseAsync(response: EdenResponse) {
    if (response is EdenResponse.Success) {
        response.entity
                .retype<T>()
                ?.let(::cacheEntityAsync)
                ?: debugLog(LogCategory.CACHE, "cannot cache $response, because of incompatible types")
    }
}