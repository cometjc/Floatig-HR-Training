package com.example.floatinghr.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class SentryTelemetryTest {
    @Test
    fun recordsMonitoringServiceBreadcrumbsWithOverlayState() {
        val sink = RecordingSentryEventSink()
        val telemetry = SentryTelemetry(sink)

        telemetry.monitoringServicesStarting(overlayPermissionGranted = true)
        telemetry.monitoringServicesStarted(overlayStarted = true)
        telemetry.monitoringServicesStopped()

        assertEquals(
            listOf(
                RecordedBreadcrumb(
                    category = "training.service",
                    message = "Starting monitoring services",
                    data = mapOf("overlayPermissionGranted" to "true")
                ),
                RecordedBreadcrumb(
                    category = "training.service",
                    message = "Monitoring services started",
                    data = mapOf("overlayStarted" to "true")
                ),
                RecordedBreadcrumb(
                    category = "training.service",
                    message = "Monitoring services stopped",
                    data = emptyMap()
                )
            ),
            sink.breadcrumbs
        )
    }

    @Test
    fun capturesTrainingErrorWithContext() {
        val sink = RecordingSentryEventSink()
        val telemetry = SentryTelemetry(sink)
        val error = IllegalStateException("BLE connection lost")

        telemetry.captureTrainingError(
            error = error,
            operation = "ble.connect",
            details = mapOf("device" to "Polar H10")
        )

        assertEquals(
            listOf(
                RecordedException(
                    throwable = error,
                    operation = "ble.connect",
                    details = mapOf("device" to "Polar H10")
                )
            ),
            sink.exceptions
        )
    }
}

private class RecordingSentryEventSink : SentryEventSink {
    val breadcrumbs = mutableListOf<RecordedBreadcrumb>()
    val exceptions = mutableListOf<RecordedException>()

    override fun addBreadcrumb(
        category: String,
        message: String,
        data: Map<String, String>
    ) {
        breadcrumbs += RecordedBreadcrumb(category, message, data)
    }

    override fun captureException(
        throwable: Throwable,
        operation: String,
        details: Map<String, String>
    ) {
        exceptions += RecordedException(throwable, operation, details)
    }
}

private data class RecordedBreadcrumb(
    val category: String,
    val message: String,
    val data: Map<String, String>
)

private data class RecordedException(
    val throwable: Throwable,
    val operation: String,
    val details: Map<String, String>
)
