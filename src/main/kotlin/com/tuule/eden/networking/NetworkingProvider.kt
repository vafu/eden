package com.tuule.eden.networking

interface NetworkingProvider {
    fun performRequest(request: Request, callback: NetworkCompletionCallback): RequestInFlight
}

interface RequestInFlight {
    fun cancel()
    val transferMetrics: RequestTransferMetrics
}

data class RequestTransferMetrics(var requestBytesSent: Int,
                                  var requestBytesTotal: Int?,
                                  var responseBytesReceived: Int,
                                  var responseBytesTotal: Int?)


typealias NetworkCompletionCallback = (Response?, Throwable?) -> Unit