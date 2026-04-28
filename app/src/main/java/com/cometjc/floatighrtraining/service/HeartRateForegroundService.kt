package com.cometjc.floatighrtraining.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cometjc.floatighrtraining.R
import com.cometjc.floatighrtraining.model.TrainingPlan
import com.cometjc.floatighrtraining.workout.WorkoutSessionState
import com.cometjc.floatighrtraining.workout.WorkoutSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HeartRateForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(NOTIFICATION_ID, notification("Waiting for heart rate belt"))
        serviceScope.launch {
            sessionStore.state.collectLatest { state ->
                startForeground(NOTIFICATION_ID, notification(state.notificationText()))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = sessionStore.state.value
        startForeground(NOTIFICATION_ID, notification(state.notificationText()))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun notification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_heart)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Heart rate monitoring",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val EXTRA_BPM = "bpm"
        const val EXTRA_STATE = "state"
        private const val CHANNEL_ID = "heart_rate_monitor"
        private const val NOTIFICATION_ID = 1001
        private val sessionStore = WorkoutSessionStore()

        val workoutSessionState = sessionStore.state

        fun startSession(training: TrainingPlan, connectedDeviceName: String? = null) {
            sessionStore.start(training, connectedDeviceName)
        }

        fun recordTelemetry(bpm: Int, cadence: Int, elapsedSeconds: Int) {
            sessionStore.recordTelemetry(bpm, cadence, elapsedSeconds)
        }

        fun stopSession() {
            sessionStore.stop()
        }
    }
}

private fun WorkoutSessionState.notificationText(): String {
    if (!isRunning) return "Waiting for heart rate belt"
    return "${bpm} BPM - ${alertState.label}"
}
