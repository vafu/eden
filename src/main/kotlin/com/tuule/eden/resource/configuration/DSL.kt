package com.tuule.eden.resource.configuration

import com.tuule.eden.resource.Resource
import com.tuule.eden.service.ResourceService

fun Resource<*>.configuration(init: ConfigMutators.() -> Unit) = ConfigMutators().also(init)

fun ResourceService.configuration(init: ConfigMutatorsBuilder.() -> Unit) =
        ConfigMutatorsBuilder().also(init)

fun ConfigMutatorsBuilder.matches(matcher: (Resource<*>) -> Boolean, init: ConfigMutators.() -> Unit) =
        addForMatcher(matcher,
                ConfigMutators().also(init).asSingleMutator())

fun ConfigMutatorsBuilder.matchesUrl(regex: String, init: ConfigMutators.() -> Unit) =
        matches({ r -> regex.toRegex().containsMatchIn(r.url) }, init)
