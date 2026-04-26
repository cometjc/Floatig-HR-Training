package com.example.floatinghr.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.floatinghr.R

class FloatingHeartRateService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        showOverlay(
            bpm = intent?.getIntExtra(EXTRA_BPM, 127) ?: 127,
            zone = intent?.getStringExtra(EXTRA_ZONE) ?: "Z2",
            state = intent?.getStringExtra(EXTRA_STATE) ?: "目標"
        )
        return START_STICKY
    }

    override fun onDestroy() {
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
        super.onDestroy()
    }

    private fun showOverlay(bpm: Int, zone: String, state: String) {
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = manager
        overlayView?.let { manager.removeView(it) }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(28, 14, 28, 14)
            setBackgroundResource(R.drawable.floating_bar_background)
        }
        val text = TextView(this).apply {
            text = "♥ $bpm BPM｜$zone｜$state"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
        }
        layout.addView(text)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        layout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    manager.updateViewLayout(layout, params)
                    true
                }
                else -> false
            }
        }

        overlayView = layout
        manager.addView(layout, params)
    }

    companion object {
        const val EXTRA_BPM = "extra_bpm"
        const val EXTRA_ZONE = "extra_zone"
        const val EXTRA_STATE = "extra_state"
    }
}
