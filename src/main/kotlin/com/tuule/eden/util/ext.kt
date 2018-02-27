package com.tuule.eden.util

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog
import com.tuule.eden.resource.Entity
import java.nio.charset.Charset

fun <T> debugLogWithValue(logCategory: LogCategory, message: String, logValue: Boolean = true, clojure: () -> T) =
        clojure().also {
            debugLog(logCategory, message)
            if (logValue && it != null) debugLog(logCategory, "Value is $it")
        }

fun Entity<ByteArray>.decodeToString() = try {
    content.toString(charset.asCharset())
} catch (e: Exception) {
    null
}

private fun String?.asCharset() =
        try {
            Charset.forName(this)
        } catch (e: Exception) {
            Charsets.UTF_8
        }

internal fun <T> T.log(message: String? = null) = also { message?.let(::print); println(" $this") }

fun <T> Iterable<(T) -> T>.mutate(initial: T) = fold(initial) { acc, mut -> mut(acc) }