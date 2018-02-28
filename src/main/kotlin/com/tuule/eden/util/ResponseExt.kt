package com.tuule.eden.util

import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.networking.ResponseInfo
import com.tuule.eden.networking.request.RequestError
import com.tuule.eden.resource.Entity


internal fun RequestError.Cause.asFailureResponse(entity: Entity<Any>? = null) =
        EdenResponse.Failure(RequestError(message, this, entity))

internal fun RequestError.asResponseInfo() = ResponseInfo(EdenResponse.Failure(this))

internal fun Throwable.asFailureResponse(entity: Entity<Any>? = null) =
        EdenResponse.Failure(RequestError(message, RequestError.Cause.User(this), entity))

internal fun Throwable.asResponseInfo() = ResponseInfo(asFailureResponse())

fun EdenResponse.asSuccessResponse() = this as? EdenResponse.Success