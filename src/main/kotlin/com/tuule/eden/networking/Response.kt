package com.tuule.eden.networking

class Response(val body: ByteArray?,
               val code: Int,
               val headers: Map<String, String>,
               val message: String?)