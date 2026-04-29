package com.cometjc.floatighrtraining.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import com.cometjc.floatighrtraining.data.persistence.AlertPreferences
import com.cometjc.floatighrtraining.data.persistence.OverlayPreferences
import com.cometjc.floatighrtraining.data.persistence.UserPreferences
import com.cometjc.floatighrtraining.data.persistence.userPreferencesDataStore
import com.cometjc.floatighrtraining.prediction.PacingDecision
import com.cometjc.floatighrtraining.telemetry.SentryTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class FloatingHeartRateService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: FloatingZoneBarView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lastAlertDecision: PacingDecision? = null
    private var lastAlertAtMillis: Long = 0
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferencesStore by lazy { applicationContext.userPreferencesDataStore() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            combine(
                HeartRateForegroundService.workoutSessionState,
                preferencesStore.preferences
            ) { session, userPrefs -> session to userPrefs }
                .collectLatest { (session, userPrefs) ->
                    if (!Settings.canDrawOverlays(this@FloatingHeartRateService)) {
                        removeOverlayIfPresent()
                        return@collectLatest
                    }
                    if (!session.isRunning) {
                        removeOverlayIfPresent()
                        return@collectLatest
                    }
                    if (!userPrefs.overlayPreferences.floatingModeEnabled) {
                        removeOverlayIfPresent()
                        return@collectLatest
                    }
                    val density = resources.displayMetrics.density
                    val state = session.toFloatingZoneBarState()
                        .withOverlayVisual(userPrefs.overlayPreferences, density)
                    showOrUpdateOverlay(state, userPrefs.overlayPreferences)
                    maybeAlert(state.decision, userPrefs.alertPreferences)
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            SentryTelemetry.instance.overlayPermissionMissingOnStart()
            stopSelf()
            return START_NOT_STICKY
        }

        val session = HeartRateForegroundService.workoutSessionState.value
        serviceScope.launch {
            val userPrefs = preferencesStore.preferences.first()
            if (!userPrefs.overlayPreferences.floatingModeEnabled) {
                removeOverlayIfPresent()
                stopSelf()
                return@launch
            }
            val density = resources.displayMetrics.density
            val state = if (session.isRunning) {
                session.toFloatingZoneBarState().withOverlayVisual(userPrefs.overlayPreferences, density)
            } else {
                FloatingZoneBarState.fromIntent(intent)
            }
            showOrUpdateOverlay(state, userPrefs.overlayPreferences)
            maybeAlert(state.decision, userPrefs.alertPreferences)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlayIfPresent()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun removeOverlayIfPresent() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
                .onFailure { SentryTelemetry.instance.captureTrainingError(it, "overlay.remove") }
        }
        overlayView = null
        layoutParams = null
    }

    private fun showOrUpdateOverlay(state: FloatingZoneBarState, overlayPrefs: OverlayPreferences) {
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = manager

        val transparency = overlayPrefs.transparencyPercent.coerceIn(0, 100)
        val windowAlpha = (1f - transparency / 100f * 0.5f).coerceIn(0.52f, 1f)

        val existing = overlayView
        if (existing != null) {
            existing.update(state)
            layoutParams?.let { params ->
                applyOverlayPosition(params, overlayPrefs)
                params.alpha = windowAlpha
                runCatching { manager.updateViewLayout(existing, params) }
                    .onFailure { SentryTelemetry.instance.captureTrainingError(it, "overlay.update_layout") }
            }
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
            applyOverlayPosition(this, overlayPrefs)
            alpha = windowAlpha
        }

        attachDragHandler(view, params, manager)
        overlayView = view
        layoutParams = params
        runCatching { manager.addView(view, params) }
            .onFailure {
                SentryTelemetry.instance.captureTrainingError(it, "overlay.add_view")
                overlayView = null
                layoutParams = null
            }
    }

    private fun applyOverlayPosition(params: WindowManager.LayoutParams, overlayPrefs: OverlayPreferences) {
        val metrics = resources.displayMetrics
        val screenW = metrics.widthPixels.coerceAtLeast(1)
        val screenH = metrics.heightPixels.coerceAtLeast(1)
        val ax = overlayPrefs.anchorX.coerceIn(0f, 1f)
        val ay = overlayPrefs.anchorY.coerceIn(0f, 1f)
        params.x = (screenW * (ax - 0.5f)).roundToInt()
        params.y = (screenH * ay).toInt().coerceIn(0, (screenH * 0.92f).toInt().coerceAtLeast(1))
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
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val metrics = resources.displayMetrics
                    val screenW = metrics.widthPixels.coerceAtLeast(1).toFloat()
                    val screenH = metrics.heightPixels.coerceAtLeast(1).toFloat()
                    val centerXNorm = ((screenW / 2f + params.x) / screenW).coerceIn(0f, 1f)
                    val topYNorm = (params.y / screenH).coerceIn(0f, 1f)
                    serviceScope.launch(Dispatchers.IO) {
                        runCatching {
                            preferencesStore.updateOverlayAnchors(
                                anchorX = centerXNorm,
                                anchorY = topYNorm
                            )
                        }.onFailure {
                            SentryTelemetry.instance.captureTrainingError(it, "overlay.persist_anchor")
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun maybeAlert(decision: PacingDecision, alertPrefs: AlertPreferences) {
        val now = System.currentTimeMillis()
        val changed = decision != lastAlertDecision
        val repeated = now - lastAlertAtMillis > ALERT_REPEAT_INTERVAL_MS
        if (!decision.shouldAlert || (!changed && !repeated)) return
        if (!decision.passesMasterToggles(alertPrefs)) return

        lastAlertDecision = decision
        lastAlertAtMillis = now
        if (decision.shouldVibrate(alertPrefs)) vibrate(decision)
        if (decision.shouldPlayTone(alertPrefs)) playTone(decision)
    }

    private fun vibrate(decision: PacingDecision) {
        if (decision == PacingDecision.Maintain) return
        val pattern = when (decision) {
            PacingDecision.SpeedUp -> longArrayOf(0, 90, 70, 90)
            PacingDecision.SlowDownSoon -> longArrayOf(0, 180, 90, 120)
            PacingDecision.SlowDownNow -> longArrayOf(0, 320, 100, 320, 100, 180)
            else -> return
        }
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }.onFailure { SentryTelemetry.instance.captureTrainingError(it, "alert.vibrate") }
    }

    private fun playTone(decision: PacingDecision) {
        if (decision == PacingDecision.Maintain) return
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
        val toneType: Int = when (decision) {
            PacingDecision.SpeedUp -> ToneGenerator.TONE_PROP_BEEP
            PacingDecision.SlowDownSoon -> ToneGenerator.TONE_PROP_ACK
            PacingDecision.SlowDownNow -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
            else -> {
                tone.release()
                return
            }
        }
        runCatching {
            tone.startTone(toneType, if (decision == PacingDecision.SlowDownNow) 240 else 140)
            overlayView?.postDelayed({ tone.release() }, 300)
        }.onFailure {
            SentryTelemetry.instance.captureTrainingError(it, "alert.tone")
            tone.release()
        }
    }

    private val PacingDecision.shouldAlert: Boolean
        get() = this != PacingDecision.Maintain

    private fun PacingDecision.passesMasterToggles(prefs: AlertPreferences): Boolean {
        return when (this) {
            PacingDecision.Maintain -> false
            PacingDecision.SpeedUp -> prefs.tooLowEnabled
            PacingDecision.SlowDownSoon, PacingDecision.SlowDownNow -> prefs.tooHighEnabled
        }
    }

    private fun PacingDecision.shouldVibrate(prefs: AlertPreferences): Boolean {
        if (!prefs.vibrationEnabled) return false
        return when (this) {
            PacingDecision.Maintain -> false
            PacingDecision.SpeedUp -> prefs.tooLowVibrationEnabled
            PacingDecision.SlowDownSoon, PacingDecision.SlowDownNow -> prefs.tooHighVibrationEnabled
        }
    }

    private fun PacingDecision.shouldPlayTone(prefs: AlertPreferences): Boolean {
        if (!prefs.soundEnabled) return false
        return when (this) {
            PacingDecision.Maintain -> false
            PacingDecision.SpeedUp -> prefs.tooLowSoundEnabled
            PacingDecision.SlowDownSoon, PacingDecision.SlowDownNow -> prefs.tooHighSoundEnabled
        }
    }

    companion object {
        const val EXTRA_BPM = "extra_bpm"
        const val EXTRA_ZONE = "extra_zone"
        const val EXTRA_TARGET_ZONE_ID = "extra_target_zone_id"
        const val EXTRA_DECISION = "extra_decision"
        const val EXTRA_STATE = "extra_state"
        private const val ALERT_REPEAT_INTERVAL_MS = 8_000L
    }
}
