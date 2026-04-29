package com.cometjc.floatighrtraining.telemetry

import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel

interface SentryEventSink {
    fun addBreadcrumb(
        category: String,
        message: String,
        data: Map<String, String> = emptyMap(),
        level: SentryLevel = SentryLevel.INFO
    )

    fun captureException(
        throwable: Throwable,
        operation: String,
        details: Map<String, String> = emptyMap()
    )
}

class SentryTelemetry(
    private val sink: SentryEventSink = SentrySdkEventSink
) {
    companion object {
        val instance = SentryTelemetry()
    }

    fun monitoringServicesStarting(overlayPermissionGranted: Boolean) {
        sink.addBreadcrumb(
            category = "training.service",
            message = "Starting monitoring services",
            data = mapOf("overlayPermissionGranted" to overlayPermissionGranted.toString())
        )
    }

    fun monitoringServicesStarted(overlayStarted: Boolean) {
        sink.addBreadcrumb(
            category = "training.service",
            message = "Monitoring services started",
            data = mapOf("overlayStarted" to overlayStarted.toString())
        )
    }

    fun monitoringServicesStopped() {
        sink.addBreadcrumb(
            category = "training.service",
            message = "Monitoring services stopped"
        )
    }

    fun overlaySkippedUserDisabled() {
        sink.addBreadcrumb(
            category = "training.overlay",
            message = "Overlay not started (user disabled floating mode)",
            level = SentryLevel.INFO
        )
    }

    fun overlayPermissionMissingOnStart() {
        sink.addBreadcrumb(
            category = "training.overlay",
            message = "Overlay service started without draw-overlays permission",
            level = SentryLevel.WARNING
        )
    }

    fun bleScanStarted() {
        sink.addBreadcrumb(
            category = "training.ble",
            message = "BLE scan started",
            level = SentryLevel.INFO
        )
    }

    fun bleScanStopped() {
        sink.addBreadcrumb(
            category = "training.ble",
            message = "BLE scan stopped"
        )
    }

    fun bleConnectAttempt() {
        sink.addBreadcrumb(
            category = "training.ble",
            message = "BLE connect attempt",
            level = SentryLevel.INFO
        )
    }

    fun bleDisconnected() {
        sink.addBreadcrumb(
            category = "training.ble",
            message = "BLE disconnected"
        )
    }

    fun foregroundServiceStarted() {
        sink.addBreadcrumb(
            category = "training.service",
            message = "Foreground service startForeground succeeded"
        )
    }

    fun foregroundServiceStartFailed(error: Throwable) {
        sink.captureException(error, "foreground_service.start", emptyMap())
    }

    fun workoutSessionStarted(segmentCount: Int) {
        sink.addBreadcrumb(
            category = "training.workout",
            message = "Workout session started",
            data = mapOf("segmentCount" to segmentCount.toString())
        )
    }

    fun workoutSessionStopped() {
        sink.addBreadcrumb(
            category = "training.workout",
            message = "Workout session stopped"
        )
    }

    fun workoutSegmentChanged(index: Int) {
        sink.addBreadcrumb(
            category = "training.workout",
            message = "Workout segment changed",
            data = mapOf("segmentIndex" to index.toString())
        )
    }

    fun runAppColdStartSpan(block: () -> Unit) {
        val transaction = Sentry.startTransaction("app.cold_start", "app.lifecycle")
        try {
            block()
        } finally {
            transaction.finish()
        }
    }

    fun runBleOperationSpan(operation: String, block: () -> Unit) {
        val span = Sentry.getSpan()?.startChild("ble", operation) ?: return block()
        try {
            block()
        } finally {
            span.finish()
        }
    }

    fun runWorkoutLifecycleSpan(operation: String, block: () -> Unit) {
        val span = Sentry.getSpan()?.startChild("workout", operation) ?: return block()
        try {
            block()
        } finally {
            span.finish()
        }
    }

    fun captureTrainingError(
        error: Throwable,
        operation: String,
        details: Map<String, String> = emptyMap()
    ) {
        sink.captureException(error, operation, details)
    }

    fun captureBleError(error: Throwable, operation: String) {
        sink.captureException(error, operation, mapOf("domain" to "ble"))
    }
}

object SentrySdkEventSink : SentryEventSink {
    override fun addBreadcrumb(
        category: String,
        message: String,
        data: Map<String, String>,
        level: SentryLevel
    ) {
        Sentry.addBreadcrumb(
            Breadcrumb().apply {
                this.category = category
                this.message = message
                this.level = level
                data.forEach { (key, value) -> setData(key, value) }
            }
        )
    }

    override fun captureException(
        throwable: Throwable,
        operation: String,
        details: Map<String, String>
    ) {
        Sentry.withScope { scope ->
            scope.setTag("operation", operation)
            details.forEach { (key, value) -> scope.setExtra(key, value) }
            Sentry.captureException(throwable)
        }
    }
}
