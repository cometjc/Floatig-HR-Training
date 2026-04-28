package com.cometjc.floatighrtraining.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.cometjc.floatighrtraining.prediction.PacingDecision
import kotlin.math.roundToInt

data class FloatingZoneSegment(
    val id: String,
    val minBpm: Int,
    val maxBpm: Int,
    val color: Int
)

data class FloatingZoneBarState(
    val bpm: Int,
    val targetZoneId: String,
    val decision: PacingDecision,
    val zones: List<FloatingZoneSegment> = DefaultFloatingZones
) {
    companion object {
        fun fromIntent(intent: android.content.Intent?): FloatingZoneBarState {
            val bpm = intent?.getIntExtra(FloatingHeartRateService.EXTRA_BPM, 127) ?: 127
            val targetZoneId = intent?.getStringExtra(FloatingHeartRateService.EXTRA_TARGET_ZONE_ID)
                ?: intent?.getStringExtra(FloatingHeartRateService.EXTRA_ZONE)
                ?: "Z2"
            val decisionName = intent?.getStringExtra(FloatingHeartRateService.EXTRA_DECISION)
            val legacyState = intent?.getStringExtra(FloatingHeartRateService.EXTRA_STATE)
            val decision = decisionName?.let { runCatching { PacingDecision.valueOf(it) }.getOrNull() }
                ?: legacyState.toDecision()
            return FloatingZoneBarState(
                bpm = bpm,
                targetZoneId = targetZoneId,
                decision = decision
            )
        }

        private fun String?.toDecision(): PacingDecision = when (this) {
            "加快步伐", "預先加速", "SpeedUp" -> PacingDecision.SpeedUp
            "提前放慢", "預先減速", "SlowDownSoon" -> PacingDecision.SlowDownSoon
            "放慢步伐", "立即減速", "SlowDownNow" -> PacingDecision.SlowDownNow
            else -> PacingDecision.Maintain
        }
    }
}

class FloatingZoneBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var state = FloatingZoneBarState(
        bpm = 127,
        targetZoneId = "Z2",
        decision = PacingDecision.Maintain
    )

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        textPaint.color = Color.WHITE
        textPaint.textSize = 34f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        indicatorPaint.color = Color.WHITE
        indicatorPaint.strokeWidth = 7f
        indicatorPaint.strokeCap = Paint.Cap.ROUND
        dimPaint.color = Color.argb(138, 0, 0, 0)
    }

    fun update(newState: FloatingZoneBarState) {
        state = newState
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(520, 136)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val zones = state.zones
        if (zones.isEmpty()) return
        val barLeft = 38f
        val barTop = 54f
        val barRight = width - 38f
        val barBottom = 82f
        val radius = 14f

        zones.forEach { zone ->
            val left = FloatingZoneBarGeometry.positionForBpm(zone.minBpm, zones, barLeft, barRight)
            val right = FloatingZoneBarGeometry.positionForBpm(zone.maxBpm, zones, barLeft, barRight)
            val active = zone.id == state.targetZoneId
            barPaint.shader = LinearGradient(
                left,
                barTop,
                right,
                barBottom,
                lighten(zone.color, if (active) 0.2f else 0.0f),
                zone.color,
                Shader.TileMode.CLAMP
            )
            barPaint.alpha = if (active) 255 else 88
            rect.set(left, barTop, right, barBottom)
            if (active) {
                glowPaint.color = alertGlowColor(state.decision)
                glowPaint.setShadowLayer(22f, 0f, 0f, glowPaint.color)
                canvas.drawRoundRect(rect, radius, radius, glowPaint)
            }
            canvas.drawRoundRect(rect, radius, radius, barPaint)
            if (!active) canvas.drawRoundRect(rect, radius, radius, dimPaint)
        }
        barPaint.shader = null
        barPaint.alpha = 255

        val indicatorX = FloatingZoneBarGeometry.positionForBpm(state.bpm, zones, barLeft, barRight)
        indicatorPaint.setShadowLayer(12f, 0f, 0f, Color.WHITE)
        canvas.drawLine(indicatorX, barTop - 14f, indicatorX, barBottom + 14f, indicatorPaint)

        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("${state.bpm} BPM", 38f, 36f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = alertGlowColor(state.decision)
        canvas.drawText(state.decision.overlayLabel(), width - 38f, 36f, textPaint)
        textPaint.color = Color.WHITE
    }

    private fun lighten(color: Int, amount: Float): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.rgb(
            (red + (255 - red) * amount).roundToInt(),
            (green + (255 - green) * amount).roundToInt(),
            (blue + (255 - blue) * amount).roundToInt()
        )
    }
}

object FloatingZoneBarGeometry {
    fun positionForBpm(
        bpm: Int,
        zones: List<FloatingZoneSegment>,
        barLeft: Float,
        barRight: Float
    ): Float {
        if (zones.isEmpty()) return barLeft
        val minBpm = zones.first().minBpm
        val maxBpm = zones.last().maxBpm
        val span = (maxBpm - minBpm).coerceAtLeast(1).toFloat()
        val fraction = (bpm.coerceIn(minBpm, maxBpm) - minBpm) / span
        return barLeft + fraction * (barRight - barLeft)
    }
}

fun PacingDecision.overlayLabel(): String = when (this) {
    PacingDecision.SpeedUp -> "預先加速"
    PacingDecision.Maintain -> "目標中"
    PacingDecision.SlowDownSoon -> "預先減速"
    PacingDecision.SlowDownNow -> "立即減速"
}

fun alertGlowColor(decision: PacingDecision): Int = when (decision) {
    PacingDecision.SpeedUp -> Color.rgb(79, 195, 247)
    PacingDecision.Maintain -> Color.rgb(55, 201, 107)
    PacingDecision.SlowDownSoon -> Color.rgb(255, 179, 0)
    PacingDecision.SlowDownNow -> Color.rgb(255, 69, 58)
}

val DefaultFloatingZones = listOf(
    FloatingZoneSegment("Z1", 91, 110, 0xFF6D7377.toInt()),
    FloatingZoneSegment("Z2", 110, 128, 0xFF4FC3F7.toInt()),
    FloatingZoneSegment("Z3", 128, 146, 0xFF49C264.toInt()),
    FloatingZoneSegment("Z4", 146, 165, 0xFFFF7043.toInt()),
    FloatingZoneSegment("Z5", 165, 183, 0xFF7E57C2.toInt())
)
