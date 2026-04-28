package com.example.floatinghr.telemetry

import io.sentry.Breadcrumb
import io.sentry.Sentry

interface SentryEventSink {
    fun addBreadcrumb(
        category: String,
        message: String,
        data: Map<String, String> = emptyMap()
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

    fun captureTrainingError(
        error: Throwable,
        operation: String,
        details: Map<String, String> = emptyMap()
    ) {
        sink.captureException(error, operation, details)
    }
}

object SentrySdkEventSink : SentryEventSink {
    override fun addBreadcrumb(
        category: String,
        message: String,
        data: Map<String, String>
    ) {
        Sentry.addBreadcrumb(
            Breadcrumb().apply {
                this.category = category
                this.message = message
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
