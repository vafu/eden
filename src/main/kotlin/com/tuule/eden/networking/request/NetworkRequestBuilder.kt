package com.tuule.eden.networking.request

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.networking.EdenResponse
import com.tuule.eden.networking.HTTPResponse
import com.tuule.eden.networking.RequestInFlight
import com.tuule.eden.networking.ResponseInfo
import com.tuule.eden.resource.Entity
import com.tuule.eden.resource.Resource
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

internal class NetworkRequestBuilder(private val resource: Resource<Any>,
                                     private val requestProducer: () -> HTTPRequest) : DefaultRequestBuilder {


    private val httpRequest = requestProducer()

    private val description = "${httpRequest.method} → ${httpRequest.url}"
    private val headersDescr = "\nHeaders: ${httpRequest.headers}"

    private var wasCancelled = false

//    val isCompleted: Boolean
//        get() =
//            responseCallbacks.completedValue != nil


    private val responseCallbacks = Callbacks<ResponseInfo>()

    override fun addResponseCallback(callback: ResponseCallback) {
        responseCallbacks.addCallback(callback)
    }

    override fun repeated() = NetworkRequestBuilder(resource, requestProducer)

    private var requestInFlight: RequestInFlight? = null
    override suspend fun start(): RequestInFlight? {
        when {
            requestInFlight != null -> {
                debugLog(LogCategory.NETWORK_DETAILS, "$description already started")
                return null
            }

            wasCancelled -> {
                debugLog(LogCategory.NETWORK, "$description won't start because it was canceled")
                return null
            }
        }

        debugLog(LogCategory.NETWORK, description)
        debugLog(LogCategory.NETWORK_DETAILS, headersDescr)

        async {
            resource.service.networkingProvider.performRequest(httpRequest, ::responseReceived)
        }

        return requestInFlight
    }


    private fun responseReceived(response: HTTPResponse?, error: Throwable?) {
        debugLog(LogCategory.NETWORK, "Response: ${response?.code ?: error?.message}")
        debugLog(LogCategory.NETWORK_DETAILS, "Raw response headers: ${response?.headers}")
        debugLog(LogCategory.NETWORK_DETAILS, "Raw response body: ${response?.body?.size} bytes")

        parseResponse(response, error)


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
}


internal fun Throwable.asResponseInfo() = ResponseInfo(EdenResponse.Failure(this))

internal fun HTTPResponse.isError() = code >= 400