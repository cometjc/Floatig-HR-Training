package com.cometjc.floatighrtraining.cadence

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidStepCadenceMonitor(
    context: Context,
    private val estimator: StepCadenceEstimator = StepCadenceEstimator()
) : CadenceMonitor, SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val stepCounter = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val _cadenceSpm = MutableStateFlow<Int?>(null)
    private var isTracking = false
    private var lastStepCount: Float? = null

    override val cadenceSpm: StateFlow<Int?> = _cadenceSpm.asStateFlow()

    override fun startTracking() {
        if (isTracking || stepCounter == null || sensorManager == null) return
        isTracking = sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun stopTracking() {
        if (!isTracking || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isTracking = false
        lastStepCount = null
    }

    override fun close() {
        stopTracking()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentCount = event.values.firstOrNull() ?: return
        val previousCount = lastStepCount
        lastStepCount = currentCount
        if (previousCount == null) return

        val newSteps = (currentCount - previousCount).toInt().coerceAtLeast(0)
        repeat(newSteps) {
            estimator.onStep(SystemClock.elapsedRealtime())?.let { cadence ->
                _cadenceSpm.value = cadence
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
