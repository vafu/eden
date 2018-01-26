package com.tuule.eden.multiplatform

import java.util.*


interface WeakCache<in K, V> {
    operator fun get(key: K): V?
    fun getOrPut(key: K, defaultValue: () -> V): V
    fun clear()
}

class WeakCacheJVM<in K, V> : WeakCache<K, V> {
    private val _weakHashMap = WeakHashMap<K, V>()

    override fun get(key: K) = _weakHashMap[key]

    override fun getOrPut(key: K, defaultValue: () -> V) = _weakHashMap.getOrPut(key, defaultValue)

    override fun clear() = _weakHashMap.clear()
}