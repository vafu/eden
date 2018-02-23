package com.tuule.eden.networking.request

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.networking.*
import com.tuule.eden.pipeline.processAndCache
import com.tuule.eden.resource.Entity
import com.tuule.eden.resource.Resource
import com.tuule.eden.util.debugLogWithValue
import com.tuule.eden.util.exceptions.BugException
import kotlinx.coroutines.experimental.async


internal interface DefaultRequest : Request {
    fun addResponseCallback(callback: (ResponseInfo) -> Unit)

    override fun onSuccess(callback: (Entity<Any>) -> Unit) = addResponseCallback { info ->
        (info.response as? EdenResponse.Success)?.let { response ->
            callback(response.entity)
        }
    }.let { this }

    override fun onCompletion(callback: (ResponseInfo) -> Unit) =
            addResponseCallback(callback)
                    .let { this }

    override fun onNewData(callback: (Entity<Any>) -> Unit) = addResponseCallback { info ->
        (info.response as? EdenResponse.Success)
                ?.takeIf { info.isNew }
                ?.entity
                ?.let(callback)
    }.let { this }

    override fun onNotModified(callback: () -> Unit) = addResponseCallback { info ->
        (info.response as? EdenResponse.Success)
                ?.takeUnless { info.isNew }
                ?.let { callback() }
    }.let { this }

    override fun onFailure(callback: () -> Unit) = addResponseCallback { info ->
        (info.response as? EdenResponse.Failure)
                ?.let { callback() }
    }.let { this }
}

typealias ResponseCallback = (ResponseInfo) -> Unit

private sealed class NetworkRequestStatus {
    class InFlight(val requestInFlight: RequestInFlight) : NetworkRequestStatus()
    class Finished(val responseInfo: ResponseInfo) : NetworkRequestStatus()
    object Canceled : NetworkRequestStatus()
    object NotStarted : NetworkRequestStatus()
}

internal class NetworkRequest(private val resource: Resource<*>,
                              private val requestProducer: () -> HTTPRequest) : DefaultRequest {


    private val httpRequest = requestProducer()

    private val description = "${httpRequest.method} → ${httpRequest.url}"
    private val headersDescr = "\nHeaders: ${httpRequest.headers}"

    private var status: NetworkRequestStatus = NetworkRequestStatus.NotStarted

    private val responseCallbacks = mutableSetOf<ResponseCallback>()
    override fun addResponseCallback(callback: ResponseCallback) {
        (status as? NetworkRequestStatus.Finished)?.responseInfo?.let(callback)
                ?: responseCallbacks.add(callback)
    }

    override fun start(): Request {
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
        return this
    }

    override val isCompleted: Boolean
        get() = status != NetworkRequestStatus.NotStarted

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
                error != null -> error.toResponseInfo()
                response?.isError() == true -> RequestError(response, cause = error).toResponseInfo()
                response != null -> {
                    if (response.code == 304) {
                        resource.data?.let { ResponseInfo(EdenResponse.Success(it), false) }
                                ?: RequestError.Cause.NoLocalDataForNotModified().toResponseInfo()
                    } else {
                        ResponseInfo(EdenResponse.Success(response.toByteArrayEntity()))
                    }
                }
                else -> throw BugException("Both ERROR and response are null for $description")
            }

    private fun transformResponse(response: ResponseInfo) =
            response.takeIf { it.isNew }
                    ?.let { resource.configuration.pipeline.processAndCache(it.response, resource) }
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

    override fun repeated() = NetworkRequest(resource, requestProducer)
}

class TypedRequest<out T : Any>(resource: Resource<*>,
                                requestProducer: () -> HTTPRequest) :
        Request by NetworkRequest(resource, requestProducer) {

    fun onData(callback: (T) -> Unit) = onSuccess { e: Entity<Any> ->
        (e.content as? T)?.let(callback)
    }
}

internal fun Throwable.toResponseInfo() = ResponseInfo(asFailure())

internal fun HTTPResponse.isError() = code >= 400