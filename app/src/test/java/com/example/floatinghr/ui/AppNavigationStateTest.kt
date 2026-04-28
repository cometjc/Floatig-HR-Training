package com.example.floatinghr.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppNavigationStateTest {
    @Test
    fun defaultsToTrainingWithoutNestedDestination() {
        val state = AppNavigationState()

        assertEquals(AppTopLevelDestination.Training, state.topLevelDestination)
        assertNull(state.trainingDestination)
    }

    @Test
    fun selectsTopLevelDestinationAndClearsTrainingDestination() {
        val state = AppNavigationState()
            .showTrainingPlans()
            .selectTopLevel(AppTopLevelDestination.History)

        assertEquals(AppTopLevelDestination.History, state.topLevelDestination)
        assertNull(state.trainingDestination)
    }

    @Test
    fun promotesTrainingPlanListAndEditorToExplicitDestinations() {
        val state = AppNavigationState()
            .showTrainingPlans()
            .editTrainingPlan()

        assertEquals(AppTopLevelDestination.Training, state.topLevelDestination)
        assertEquals(TrainingDestination.PlanEditor, state.trainingDestination)
    }

    @Test
    fun dismissesNestedTrainingDestination() {
        val state = AppNavigationState()
            .showTrainingPlans()
            .dismissTrainingDestination()

        assertNull(state.trainingDestination)
    }
}
