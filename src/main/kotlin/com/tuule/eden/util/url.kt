package com.tuule.eden.util

private val urlRegex = Regex("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
private val pathRegex = Regex("[-a-zA-Z0-9+&@#/%?=~]*[-a-zA-Z0-9+&@#/%=~_|]")

fun String.asValidUrl() =
        removeSuffix("/")
                .takeIf { it.matches(urlRegex) } ?: throw RuntimeException("Bad url $this")

fun String.asValidPath() =
        removePrefix("/")
                .takeIf { this.matches(pathRegex) } ?: throw  RuntimeException("bad path $this")

fun String.addPath(path: String) = "${this.asValidUrl()}/${path.asValidPath()}"