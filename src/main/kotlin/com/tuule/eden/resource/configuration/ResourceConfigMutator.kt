package com.tuule.eden.resource.configuration

import com.tuule.eden.resource.Resource

typealias ConfigurationMutator = ResourceConfiguration.() -> ResourceConfiguration
typealias ResourceMatcher<T> = (Resource<T>) -> Boolean

private val anyResourceMatcher: ResourceMatcher<*> = { true }

class ConfigMutatorsBuilder<T : Any>(val resource: Resource<T>) : ConfigMutators() {

    private val matchers = mutableMapOf<ResourceMatcher<T>, ConfigurationMutator>()

    fun addForMatcher(matcher: ResourceMatcher<T>, mutator: ConfigurationMutator) {
        matchers.put(matcher, mutator)
    }

    fun build() =
            matchers
                    .apply {
                        put(anyResourceMatcher,
                                this@ConfigMutatorsBuilder.reduce())
                    }
                    .filterKeys { it(resource) }.values
                    .fold(ResourceConfiguration()) { acc, mutator -> mutator(acc) }
}

open class ConfigMutators {
    private val mutations = mutableListOf<ConfigurationMutator>()

    fun addMutator(mutator: ConfigurationMutator) {
        mutations.add(mutator)
    }

    internal fun reduce(): ConfigurationMutator = {
        mutations.foldRight(this) { acc, result -> acc(result) }
    }
}

