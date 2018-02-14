package com.tuule.eden.pipeline

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.networking.asSuccess
import com.tuule.eden.networking.request.RequestError
import com.tuule.eden.resource.Resource
import java.lang.reflect.Type

interface ResponseTransformer {
    fun transform(edenResponse: EdenResponse): EdenResponse
}

class GsonTransformer(type: Type) : ResponseTransformer {
    val gsonAdapter = Gson().getAdapter(TypeToken.get(type))
    override fun transform(edenResponse: EdenResponse): EdenResponse {
        val result = try {
            edenResponse.asSuccess()
                    ?.entity
                    ?.retype<String>()
                    ?.let { entity ->
                        entity.map { gsonAdapter.fromJson(entity.content) }
                    }
        } catch (exc: Exception) {
            return EdenResponse.Failure(RequestError.Cause.User(exc))
        }

        return result?.let { EdenResponse.Success(it) } ?:
                EdenResponse.Failure(RequestError.Cause.TypeMismatchError(edenResponse))
    }
}

fun Resource<*>.getTransformer(): GsonTransformer = this::class.java.genericSuperclass.let(::GsonTransformer)


