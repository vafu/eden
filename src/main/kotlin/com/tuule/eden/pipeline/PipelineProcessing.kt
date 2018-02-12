package com.tuule.eden.pipeline

import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.resource.Entity
import com.tuule.eden.resource.EntityCache
import com.tuule.eden.resource.Resource
import kotlinx.coroutines.experimental.async
import kotlin.concurrent.thread

private val Pipeline.activeStages
    get() = Pipeline.StageKey.values()
            .map(this::get)
            .filter(PipelineStage::isActive)

internal fun Pipeline.processor(rawResponse: EdenResponse, resource: Resource<*>) = {

}


internal suspend fun <T : Any> Pipeline.cachedEntity(resource: Resource<T>): Entity<T>? =
        activeStages
                .find(PipelineStage::caching)
                ?.let { it.cache  as? EntityCache<T> }
                ?.let { it[resource] }