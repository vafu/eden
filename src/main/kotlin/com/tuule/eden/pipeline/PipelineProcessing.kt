package com.tuule.eden.pipeline

private val Pipeline.stagesInOrder
    get() = Pipeline.StageKey.values().mapNotNull { this[it] }

