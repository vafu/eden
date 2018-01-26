package com.tuule.eden.networking

class Request(val url: String,
              val method: RequestMethod,
              val headers: Map<String, String>,
              val body: ByteArray)

enum class RequestMethod {
    OPTIONS,
    GET,
    HEAD,
    POST,
    PUT,
    PATCH,
    DELETE
}
