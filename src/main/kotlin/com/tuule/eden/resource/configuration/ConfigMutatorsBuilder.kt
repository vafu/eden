package com.tuule.eden.resource.configuration

import com.tuule.eden.resource.Resource

typealias ConfigurationMutator = ResourceConfiguration.() -> ResourceConfiguration
typealias ConfigurationMatcher = (Resource<*>) -> Boolean

private val allMatcher: ConfigurationMatcher = { true }

class ConfigMutatorsBuilder : ConfigMutators() {
    private val matchers = mutableMapOf<ConfigurationMatcher, ConfigurationMutator>()

    fun addForMatcher(matcher: ConfigurationMatcher, mutator: ConfigurationMutator) {
        matchers.put(matcher, mutator)
    }

    fun buildForResource(resource: Resource<*>) =
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

