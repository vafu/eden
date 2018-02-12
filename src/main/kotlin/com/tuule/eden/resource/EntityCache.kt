package com.tuule.eden.resource

import java.sql.Timestamp

//interface EntityCache<Key : Any, Type : Any> {
//    fun key(resource: Resource<Type>): Key?
//
//    operator fun get(key: Key): Entity<Any>?
//    operator fun set(key: Key, value: Entity<Any>)
//    fun remove(key: Key)
//
//    fun touch(timestamp: String, key: Key) {
//        get(key)
//                ?.run(Entity<Any>::touch)
//                ?.let { this[key] = it }
//    }
//}
//

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