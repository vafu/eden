package com.tuule.eden.resource.configuration

import com.tuule.eden.resource.Resource

//typealias ResourceMatcher<T> = (Resource<T>) -> Boolean
//
//class ResourceConfigMutator<T : Any>(val resource: Resource<T>) : ConfigurationBuilder() {
//    fun addForMatcher(matcher: ResourceMatcher<T>, mutator: ConfigurationMutator) {
//        if (matcher(resource))
//            addMutator(mutator)
//    }
//}
//
//
//open class ConfigurationBuilder {
//    private val mutations = mutableListOf<ConfigurationMutator>()
//
//    fun addMutator(mutator: ConfigurationMutator) {
//        mutations.add(mutator)
//    }
//
//    internal fun fold(): ConfigurationMutator = { build(this) }
//
//    internal fun build(): Configuration =
//            build(Configuration())
//
//    internal fun build(initial: Configuration) =
//            mutations.fold(initial)
//            { acc, mutator -> mutator(acc) }
//}
//


