package com.tuule.eden.util

private val urlRegex = Regex("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
private val pathRegex = Regex("[-a-zA-Z0-9+&@#/%?=~]*[-a-zA-Z0-9+&@#/%=~_|]")

internal fun String.asValidUrl() =
        removeSuffix("/")
                .takeIf { it.matches(urlRegex) } ?: throw IllegalArgumentException("Bad url $this")

internal fun String.asValidPath() =
        removePrefix("/")
                .takeIf { this.matches(pathRegex) } ?: throw  IllegalArgumentException("bad path $this")

internal fun String.addPath(path: String) = "${this.asValidUrl()}/${path.asValidPath()}"