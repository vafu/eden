package com.tuule.eden.networking.request

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.networking.HTTPResponse
import com.tuule.eden.networking.RequestInFlight
import com.tuule.eden.networking.ResponseInfo
import com.tuule.eden.pipeline.processAndCache
import com.tuule.eden.resource.Entity
import com.tuule.eden.resource.Resource
import com.tuule.eden.util.debugLogWithValue
import com.tuule.eden.util.exceptions.BugException
import kotlinx.coroutines.experimental.async


internal interface DefaultRequestBuilder : RequestBuilder {
    fun addResponseCallback(callback: (ResponseInfo) -> Unit)

    override fun onSuccess(callback: (Entity<Any>) -> Unit) = addResponseCallback { info ->
        (info.response as? EdenResponse.Success)?.let { response ->
            callback(response.entity)
        }
    }

    override fun onCompletion(callback: (ResponseInfo) -> Unit) = addResponseCallback(callback)

    override fun onNewData(callback: (Entity<Any>) -> Unit) = addResponseCallback { info ->
        (info.response as? EdenResponse.Success)
                ?.takeIf { info.isNew }
                ?.entity
                ?.let(callback)
    }

    override fun onNotModified(callback: () -> Unit) = addResponseCallback { info ->
        (info.response as? EdenResponse.Success)
                ?.takeUnless { info.isNew }
                ?.let { callback() }
    }

    override fun onFailure(callback: () -> Unit) = addResponseCallback { info ->
        (info.response as? EdenResponse.Failure)
                ?.let { callback() }
    }
}

typealias ResponseCallback = (ResponseInfo) -> Unit

private sealed class NetworkRequestStatus {
    class InFlight(val requestInFlight: RequestInFlight) : NetworkRequestStatus()
    class Finished(val responseInfo: ResponseInfo) : NetworkRequestStatus()
    object Canceled : NetworkRequestStatus()
    object NotStarted : NetworkRequestStatus()
}

internal class NetworkRequestBuilder(private val resource: Resource<*>,
                                     private val requestProducer: () -> HTTPRequest) : DefaultRequestBuilder {


    private val httpRequest = requestProducer()

    private val description = "${httpRequest.method} → ${httpRequest.url}"
    private val headersDescr = "\nHeaders: ${httpRequest.headers}"

    private var status: NetworkRequestStatus = NetworkRequestStatus.NotStarted

    private val responseCallbacks = mutableSetOf<ResponseCallback>()
    override fun addResponseCallback(callback: ResponseCallback) {
        responseCallbacks.add(callback)
    }

    override fun start() {
        when (status) {
            is NetworkRequestStatus.InFlight ->
                debugLog(LogCategory.NETWORK, "$description is already started")

            is NetworkRequestStatus.Finished ->
                debugLog(LogCategory.NETWORK, "$description is already finished")

            NetworkRequestStatus.Canceled ->
                debugLog(LogCategory.NETWORK, "$description was canceled")

            NetworkRequestStatus.NotStarted -> {
                debugLog(LogCategory.NETWORK, description)
                debugLog(LogCategory.NETWORK_DETAILS, headersDescr)

                resource.service.networkingProvider.startRequest(httpRequest, ::responseReceived)
                        .also { status = NetworkRequestStatus.InFlight(it) }
            }
        }
    }

    override fun cancel() =
            (status as? NetworkRequestStatus.InFlight)?.requestInFlight?.cancel()
                    ?: debugLog(LogCategory.NETWORK_DETAILS, "unable to cancel request, status is $status")


    private fun responseReceived(response: HTTPResponse?, error: Throwable?) {
        async {
            debugLog(LogCategory.NETWORK, "Response: ${response?.code ?: error?.message}")
            debugLog(LogCategory.NETWORK_DETAILS, "Raw response headers: ${response?.headers}")
            debugLog(LogCategory.NETWORK_DETAILS, "Raw response body: ${response?.body?.size} bytes")

            parseResponse(response, error)
                    .takeUnless(::shouldSkipResponse)
                    ?.let(::transformResponse)
                    ?.also(::broadcastResponse)

        }
    }

    private fun shouldSkipResponse(respInfo: ResponseInfo) =
            when (status) {
                NetworkRequestStatus.Canceled -> debugLogWithValue(LogCategory.NETWORK_DETAILS,
                        "Received response, but request was already canceled:" +
                                " $description\nNewResponse: ${respInfo.response}") { true }
                is NetworkRequestStatus.Finished -> throw BugException("Received response for request that was already completed")
                else -> respInfo.response.cancellation
            }

    private fun parseResponse(response: HTTPResponse?, error: Throwable?) =
            when {
                error != null -> error.asResponseInfo()
                response?.takeIf { it.isError() } != null -> RequestError(response, cause = error).asResponseInfo()
                response != null -> {
                    if (response.code == 304) {
                        resource.data?.let { ResponseInfo(EdenResponse.Success(it), false) }
                                ?: RequestError.Cause.NoLocalDataForNotModified().asResponseInfo()
                    } else {
                        ResponseInfo(EdenResponse.Success(Entity(response, response.body ?: byteArrayOf())))
                    }
                }
                else -> throw BugException("Both error and response are null for $description")
            }

    private fun transformResponse(response: ResponseInfo) =
            response.takeIf { it.isNew }
                    ?.let { resource._configuration.pipeline.processAndCache(it.response, resource) }
                    ?.let { ResponseInfo(it, true) }
                    ?: response


    private fun broadcastResponse(response: ResponseInfo) {
        status = NetworkRequestStatus.Finished(response)
        response.also { validResponse ->
            async {
                responseCallbacks.forEach { it(validResponse) }
            }
        }
    }

    override fun repeated() = NetworkRequestBuilder(resource, requestProducer)
}


internal fun Throwable.asResponseInfo() = ResponseInfo(EdenResponse.Failure(this))

internal fun HTTPResponse.isError() = code >= 400