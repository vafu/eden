package com.tuule.eden.multiplatform

import java.net.MalformedURLException
import java.net.URL


fun String.asURL() = try {
    URL(this).toString()
            .removeSuffix("/")

} catch (e: MalformedURLException) {
    null
}