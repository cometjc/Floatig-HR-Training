package com.cometjc.floatighrtraining.cadence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StepCadenceEstimatorTest {
    @Test
    fun returnsNullUntilAtLeastTwoStepsArrive() {
        val estimator = StepCadenceEstimator()

        assertNull(estimator.onStep(1_000L))
    }

    @Test
    fun convertsStepIntervalsIntoSpm() {
        val estimator = StepCadenceEstimator()

        estimator.onStep(0L)
        val cadence = estimator.onStep(500L)

        assertEquals(120, cadence)
    }
}
