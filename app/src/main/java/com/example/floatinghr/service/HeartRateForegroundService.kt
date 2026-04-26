package com.example.floatinghr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.floatinghr.R

class HeartRateForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(NOTIFICATION_ID, notification("Waiting for heart rate belt"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bpm = intent?.getIntExtra(EXTRA_BPM, 0)?.takeIf { it > 0 }
        val state = intent?.getStringExtra(EXTRA_STATE) ?: "Training active"
        startForeground(NOTIFICATION_ID, notification(bpm?.let { "$it BPM - $state" } ?: state))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
    }
}
