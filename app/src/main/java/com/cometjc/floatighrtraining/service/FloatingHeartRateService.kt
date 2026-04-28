package com.cometjc.floatighrtraining.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import com.cometjc.floatighrtraining.prediction.PacingDecision

class FloatingHeartRateService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: FloatingZoneBarView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lastAlertDecision: PacingDecision? = null
    private var lastAlertAtMillis: Long = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val state = FloatingZoneBarState.fromIntent(intent)
        showOrUpdateOverlay(state)
        maybeAlert(state.decision)
        return START_STICKY
    }

    override fun onDestroy() {
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
        super.onDestroy()
    }

    private fun showOrUpdateOverlay(state: FloatingZoneBarState) {
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = manager

        val existing = overlayView
        if (existing != null) {
            existing.update(state)
            return
        }

        val view = FloatingZoneBarView(this).apply {
            update(state)
        }
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

        attachDragHandler(view, params, manager)
        overlayView = view
        layoutParams = params
        manager.addView(view, params)
    }

    private fun attachDragHandler(
        view: FloatingZoneBarView,
        params: WindowManager.LayoutParams,
        manager: WindowManager
    ) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        view.setOnTouchListener { _, event ->
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
                    manager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun maybeAlert(decision: PacingDecision) {
        val now = System.currentTimeMillis()
        val changed = decision != lastAlertDecision
        val repeated = now - lastAlertAtMillis > ALERT_REPEAT_INTERVAL_MS
        if (!decision.shouldAlert || (!changed && !repeated)) return

        lastAlertDecision = decision
        lastAlertAtMillis = now
        vibrate(decision)
        playTone(decision)
    }

    private fun vibrate(decision: PacingDecision) {
        val pattern = when (decision) {
            PacingDecision.SpeedUp -> longArrayOf(0, 90, 70, 90)
            PacingDecision.Maintain -> return
            PacingDecision.SlowDownSoon -> longArrayOf(0, 180, 90, 120)
            PacingDecision.SlowDownNow -> longArrayOf(0, 320, 100, 320, 100, 180)
        }
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun playTone(decision: PacingDecision) {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
        val toneType = when (decision) {
            PacingDecision.SpeedUp -> ToneGenerator.TONE_PROP_BEEP
            PacingDecision.Maintain -> return
            PacingDecision.SlowDownSoon -> ToneGenerator.TONE_PROP_ACK
            PacingDecision.SlowDownNow -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
        }
        tone.startTone(toneType, if (decision == PacingDecision.SlowDownNow) 240 else 140)
        overlayView?.postDelayed({ tone.release() }, 300)
    }

    private val PacingDecision.shouldAlert: Boolean
        get() = this != PacingDecision.Maintain

    companion object {
        const val EXTRA_BPM = "extra_bpm"
        const val EXTRA_ZONE = "extra_zone"
        const val EXTRA_TARGET_ZONE_ID = "extra_target_zone_id"
        const val EXTRA_DECISION = "extra_decision"
        const val EXTRA_STATE = "extra_state"
        private const val ALERT_REPEAT_INTERVAL_MS = 8_000L
    }
}
