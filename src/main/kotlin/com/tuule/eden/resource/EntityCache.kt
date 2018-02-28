package com.tuule.eden.resource

interface EntityCache<Type : Any> {

    operator fun get(resource: Resource<Type>): Entity<Type>?
    operator fun set(resource: Resource<Type>, value: Entity<Type>)
    fun remove(resource: Resource<Type>)

    fun touch(timestamp: Long, resource: Resource<Type>) {
        this[resource]
                ?.run(Entity<Type>::touch)
                ?.let { this[resource] = it }
    }
}