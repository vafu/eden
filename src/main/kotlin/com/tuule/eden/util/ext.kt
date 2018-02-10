package com.tuule.eden.util

import com.tuule.eden.multiplatform.LogCategory
import com.tuule.eden.multiplatform.debugLog

fun <T> debugLogWithValue(logCategory: LogCategory, message: String, logValue: Boolean = true, clojure: () -> T) =
        clojure().also {
            debugLog(logCategory, message)
            if (logValue && it != null) debugLog(logCategory, "Value is $it")
        }
