package com.tuule.eden.pipeline

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.tuule.eden.resource.Entity
import com.tuule.eden.resource.Resource
import com.tuule.eden.util.decodeToString


fun createTextDecodingTransformer(processErrors: Boolean = true) =
        ResponseContentTransformer(processingErrors = processErrors, processor = Entity<ByteArray>::decodeToString)

fun createGsonDecodingTransformer(gson: Gson = Gson(), processErrors: Boolean = true) =
        ResponseContentTransformer<ByteArray, JsonElement>(processingErrors = processErrors) { it.decodeToString()?.let(gson::toJsonTree) }

fun <T : Any> Resource<T>.getJsonTransformer(gson: Gson = Gson()): ResponseContentTransformer<JsonElement, T> =
        ResponseContentTransformer(processingErrors = false) { entity ->
            this::class.java.genericSuperclass
                    .let { TypeToken.get(it) }
                    .let { gson.getAdapter(it) }
                    .fromJsonTree(entity.content) as? T
        }
