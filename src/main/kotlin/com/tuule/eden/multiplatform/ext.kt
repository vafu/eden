package com.tuule.eden.multiplatform

import java.net.MalformedURLException
import java.net.URL


fun String.asURL() = try {
    URL(this).toString()
            .removeSuffix("/")
            .let { "$it/" }

} catch (e: MalformedURLException) {
    null
}