package com.tuule.eden.pipeline

import com.tuule.eden.networking.EdenResponse

class Pipeline {

    private val stages = mutableMapOf<StageKey, PipelineStage>()

    operator fun get(key: StageKey) = stages[key]
    operator fun set(key: StageKey, value: PipelineStage) {
        stages[key] = value
    }

    fun removeAllTransformers() = stages.forEach { _, u -> u.clear() }

    enum class StageKey {
        RAW_DATA,
        DECODING,
        PARSING,
        MODEL,
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

    internal fun processs(response: EdenResponse) {
        transformers.fold(response) { acc, func -> func.transform(acc) }
    }

}
