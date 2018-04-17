package com.tuule.eden.pipeline

import com.google.gson.Gson
import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.resource.Resource
import java.lang.reflect.Type

//fun <T : Any> Resource<T>.getJsonTransformer(gson: Gson = Gson()) =
//        ResponseContentTransformer<String, T> {
//            try {
//                gson.fromJson(it.content, dataType)
//            } catch (e: Exception) {
//                null
//            }
//        }

fun <R : Any> getTypeTransformer(type: Type, gson: Gson = Gson()) =
        ResponseContentTransformer<String, R>(processingErrors = true) {
            try {
                gson.fromJson(it.content, type)
            } catch (e: Exception) {
                null
            }
        }

inline fun <reified R : Any> getTypeTransformer(gson: Gson = Gson()): ResponseContentTransformer<String, R> =
        getTypeTransformer(R::class.java, gson)


class ErrorExtractor<T : Any>(private val type: Class<T>) : ResponseTransformer {
    override val description = "error extraction"
    override fun transform(edenResponse: EdenResponse) = when (edenResponse) {
        is EdenResponse.Success -> edenResponse
        is EdenResponse.Failure -> getTypeTransformer<T>(type).transform(edenResponse)
    }
}