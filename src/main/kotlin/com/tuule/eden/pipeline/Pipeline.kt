package com.tuule.eden.pipeline

import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.resource.EntityCache

class Pipeline {

    private val stages = mutableMapOf<StageKey, PipelineStage>()

    operator fun get(key: StageKey) = stages.getOrPut(key, ::PipelineStage)
    operator fun set(key: StageKey, value: PipelineStage) {
        stages[key] = value
    }

    fun removeAllTransformers() = stages.forEach { _, u -> u.clear() }

    enum class StageKey {
        RAW_DATA,
        DECODING,
        PARSING,
        MODEL,
        CACHING,
        CLEANUP
    }
}

class PipelineStage {
    private val transformers = mutableSetOf<ResponseTransformer>()

    fun add(responseTransformer: ResponseTransformer) {
        transformers.add(responseTransformer)
    }

    fun clear() {
        transformers.clear()
    }

    internal val isActive
        get() = caching || transformers.isNotEmpty()

    var cache: EntityCache<*>? = null

    val caching
        get() = cache != null

    internal fun process(response: EdenResponse) =
            transformers.fold(response) { acc, func -> func.transform(acc) }
}