package com.tuule.eden.resource.configuration

import com.tuule.eden.networking.request.RequestMethod
import com.tuule.eden.resource.Resource

typealias ConfigurationMutator = Configuration.(Resource<*>) -> Configuration

internal data class ConfigurationEntry(val methods: Set<RequestMethod>,
                                       val urlMatcher: (String) -> Boolean,
                                       val description: String? = null,
                                       val configurator: ConfigurationMutator) {
    override fun toString() = description ?: super.toString()
}
