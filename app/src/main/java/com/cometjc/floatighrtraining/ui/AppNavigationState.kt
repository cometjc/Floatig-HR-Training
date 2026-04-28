package com.cometjc.floatighrtraining.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sports
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTopLevelDestination(
    val label: String,
    val icon: ImageVector
) {
    Training("Training", Icons.Default.Sports),
    History("Verlauf", Icons.Default.History),
    Settings("Einstellungen", Icons.Default.Settings)
}

enum class TrainingDestination {
    PlanList,
    PlanEditor
}

data class AppNavigationState(
    val topLevelDestination: AppTopLevelDestination = AppTopLevelDestination.Training,
    val trainingDestination: TrainingDestination? = null
) {
    fun selectTopLevel(destination: AppTopLevelDestination): AppNavigationState {
        return copy(
            topLevelDestination = destination,
            trainingDestination = null
        )
    }

    fun showTrainingPlans(): AppNavigationState {
        return copy(
            topLevelDestination = AppTopLevelDestination.Training,
            trainingDestination = TrainingDestination.PlanList
        )
    }

    fun editTrainingPlan(): AppNavigationState {
        return copy(
            topLevelDestination = AppTopLevelDestination.Training,
            trainingDestination = TrainingDestination.PlanEditor
        )
    }

    fun dismissTrainingDestination(): AppNavigationState {
        return copy(trainingDestination = null)
    }
}
