package com.tuule.eden.okhttpadapter

import com.tuule.eden.networking.*
import com.tuule.eden.networking.request.HTTPRequest
import com.tuule.eden.networking.request.RequestMethod.*
import com.tuule.eden.networking.HTTPResponse
import com.tuule.eden.resource.header
import okhttp3.*
import java.io.IOException

class OkhttpNetworkProvider(private val client: OkHttpClient = OkHttpClient()) : NetworkingProvider {

    override fun startRequest(httpRequest: HTTPRequest, callback: NetworkCompletionCallback): RequestInFlight =
            client.newCall(httpRequest.toOkHttpRequest())
                    .apply { enqueue({ callback(it.toEdenResponse(), null) }, { callback(null, it) }) }
                    .let(::OkRequestInFlight)


    private class OkRequestInFlight(private val call: Call) : RequestInFlight {
        override fun cancel() {
            call.cancel()
        }

        override val transferMetrics: RequestTransferMetrics
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    }

}

private fun HTTPRequest.getMediaType() = headers.header("content-type")?.let(MediaType::parse)

private fun HTTPRequest.okhttpRequestBody() = body?.let { RequestBody.create(getMediaType(), it) }

private fun HTTPRequest.toOkHttpRequest() = okhttp3.Request.Builder()
        .url(url)
        .apply { headers.forEach { (key, value) -> addHeader(key, value) } }
        .run {
            when (method) {
                GET -> get()
                HEAD -> head()
                POST -> okhttpRequestBody()?.let(::post)
                PUT -> okhttpRequestBody()?.let(::put)
                PATCH -> okhttpRequestBody()?.let(::patch)
                DELETE -> delete(okhttpRequestBody())
            } ?: throw RuntimeException("unable to perform method ${method.name} with empty body")
        }
        .build()

private fun okhttp3.Response.toEdenResponse() =
        HTTPResponse(body = this.body()?.bytes(),
                code = this.code(),
                headers = this.headers().toMap(),
                message = this.message())

private fun Headers.toMap() =
        toMultimap().mapValues { (_, v) -> v.joinToString(",") }

inline private fun Call.enqueue(crossinline onSuccess: (okhttp3.Response) -> Unit, crossinline onFailure: (Throwable) -> Unit) =
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                onSuccess(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                onFailure(e)
            }
        })