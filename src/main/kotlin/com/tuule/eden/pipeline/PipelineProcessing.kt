package com.tuule.eden.pipeline

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.resource.Resource

private val Pipeline.activeStages
    get() = Pipeline.StageKey.values()
            .map(this::get)
            .filter(PipelineStage::isActive)


private fun <T : Any> Pipeline.processAndCache(response: EdenResponse, resource: Resource<T>) {
    activeStages
            .fold(response) { acc, stage -> stage.process(acc) }
            .let(resource::cacheResponseAsync)
}

private fun <T : Any> Resource<T>.cacheResponseAsync(response: EdenResponse) {
    if (response is EdenResponse.Success) {
        response.entity
                .retype<T>()
                ?.let(::cacheEntityAsync)
                ?: debugLog(LogCategory.CACHE, "cannot cache $response, because of incompatible types")
    }
}