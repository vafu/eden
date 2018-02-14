package com.tuule.eden.resource.configuration

import com.tuule.eden.resource.Resource
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

typealias ConfigurationMutator = ResourceConfiguration.() -> ResourceConfiguration
typealias ConfigurationMatcher = (Resource<*>) -> Boolean

private val allMatcher: ConfigurationMatcher = { true }

class ConfigMutatorsBuilder(val resource: Resource<*>) : ConfigMutators() {

    private val matchers = mutableMapOf<ConfigurationMatcher, ConfigurationMutator>()

    fun addForMatcher(matcher: ConfigurationMatcher, mutator: ConfigurationMutator) {
        matchers.put(matcher, mutator)
    }

    fun build() =
            matchers
                    .apply { put(allMatcher, asSingleMutator()) }
                    .filterKeys { it(resource) }.values
                    .fold(ResourceConfiguration()) { acc, mutator -> mutator(acc) }
}

open class ConfigMutators {
    private val mutations = mutableListOf<ConfigurationMutator>()

    fun addMutator(mutator: ConfigurationMutator) {
        mutations.add(mutator)
    }

    internal fun asSingleMutator(): ConfigurationMutator = {
        mutations.foldRight(this) { acc, result -> acc(result) }
    }
}

