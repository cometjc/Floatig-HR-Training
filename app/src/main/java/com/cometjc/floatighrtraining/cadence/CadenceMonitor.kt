package com.cometjc.floatighrtraining.cadence

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface CadenceMonitor {
    val cadenceSpm: StateFlow<Int?>

    fun startTracking()
    fun stopTracking()
    fun close()
}

object NoopCadenceMonitor : CadenceMonitor {
    private val state = MutableStateFlow<Int?>(null)

    override val cadenceSpm: StateFlow<Int?> = state

    override fun startTracking() = Unit

    override fun stopTracking() = Unit

    override fun close() = Unit
}
