package com.tuule.eden.networking

import com.tuule.eden.networking.request.HTTPRequest

interface NetworkingProvider {
    fun performRequest(httpRequest: HTTPRequest, callback: NetworkCompletionCallback): RequestInFlight
}

interface RequestInFlight {
    fun cancel()
    val transferMetrics: RequestTransferMetrics
}

data class RequestTransferMetrics(var requestBytesSent: Int,
                                  var requestBytesTotal: Int?,
                                  var responseBytesReceived: Int,
                                  var responseBytesTotal: Int?)

typealias NetworkCompletionCallback = (HTTPResponse?, Throwable?) -> Unit