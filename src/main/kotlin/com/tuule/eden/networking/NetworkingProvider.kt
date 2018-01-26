package com.tuule.eden.networking

interface NetworkingProvider {
    fun performRequest(request: Request, callback: NetworkCompletionCallback): RequestInFlight
}

interface RequestInFlight {
    fun cancel()
    val transferMetrics: RequestTransferMetrics
}

data class RequestTransferMetrics(public var requestBytesSent: Int,
                                  public var requestBytesTotal: Int?,
                                  public var responseBytesReceived: Int,
                                  public var responseBytesTotal: Int?)


typealias NetworkCompletionCallback = (Response?, Error?) -> Unit