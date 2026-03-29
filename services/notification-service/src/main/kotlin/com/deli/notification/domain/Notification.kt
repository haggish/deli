package com.deli.notification.domain

import java.time.Instant

/**
 * Notification payload ready to be sent via FCM.
 *
 * This is an in-memory model — the notification service is stateless.
 * Sent notifications are not persisted here; the Kafka event is the
 * durable record. If a notification fails to send, it is logged and
 * the event is retried by Kafka's retry mechanism.
 */
data class PushNotification(
    val recipientFcmToken: String,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val createdAt: Instant = Instant.now(),
)

/**
 * Result of a single FCM send attempt.
 */
sealed class SendResult {
    data class Success(val messageId: String) : SendResult()
    data class Failure(val reason: String, val isRetryable: Boolean) : SendResult()
    data object DryRun : SendResult()
}
