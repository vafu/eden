package com.tuule.eden.multiplatform


enum class LogCategory(val title: String) {
    NETWORK("Network"),
    NETWORK_DETAILS("Networking details"),
    PIPELINE("Pipeline"),
    STATE_CHANGES("State changes"),
    OBSERVERS("Observers"),
    STALENESS("Staleness"),
    CACHE("Caching"),
    CONFIGURATION("Configuration")
}


sealed class LogLevel(val categories: Set<LogCategory>) {

    object None : LogLevel(emptySet())

    object Common : LogLevel(setOf(LogCategory.NETWORK, LogCategory.STATE_CHANGES, LogCategory.STALENESS))

    object Verbose : LogLevel(LogCategory.values().filter { it != LogCategory.NETWORK_DETAILS }.toSet())

    object Full : LogLevel(LogCategory.values().toSet())

    class Custom(categories: Set<LogCategory>) : LogLevel(categories)
}


object Logger {

    private val maxCategoryNameLength = LogCategory.values().map { it.title.length }.max() ?: 0

    var logLevel: LogLevel = LogLevel.Common

    var timestampEnabled = false

    internal val logFormatter = { category: LogCategory, message: String ->
        val padding = category.title.padStart(maxCategoryNameLength, '—')
        val prefix = "Eden: $padding—[${getThreadName()}]—> "
        "$prefix${message.replace("\n", "\n$prefix")}"
    }

    //todo multiplatform
    fun print(message: String) {
        println(message)
    }

    //todo multiplatform
    private fun getThreadName(): String =
            Thread.currentThread().name

}

internal fun LogCategory.isEnabled() = Logger.logLevel.categories.contains(this)

internal fun debugLog(logCategory: LogCategory, message: String) {
    if (logCategory.isEnabled()) {
        println(Logger.logFormatter(logCategory, message))
    }
}