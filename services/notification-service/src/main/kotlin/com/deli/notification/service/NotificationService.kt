package com.deli.notification.service

import com.deli.notification.domain.PushNotification
import com.deli.notification.domain.SendResult
import com.deli.notification.fcm.FcmClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val fcmClient: FcmClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ── Delivery notifications (to customer) ──────────────────────────────────

    fun notifyDeliveryConfirmed(
        customerFcmToken: String?,
        trackingNumber: String,
        placement: String,
    ) {
        val token =
            customerFcmToken ?: run {
                log.debug("No FCM token for customer — skipping delivery confirmed notification")
                return
            }

        send(
            PushNotification(
                recipientFcmToken = token,
                title = "Package delivered ✓",
                body = "Your package $trackingNumber has been delivered ($placement).",
                data =
                    mapOf(
                        "type" to "DELIVERY_CONFIRMED",
                        "trackingNumber" to trackingNumber,
                        "placement" to placement,
                    ),
            ),
        )
    }

    fun notifyDeliveryFailed(
        customerFcmToken: String?,
        trackingNumber: String,
        reason: String,
        willReschedule: Boolean,
    ) {
        val token =
            customerFcmToken ?: run {
                log.debug("No FCM token for customer — skipping delivery failed notification")
                return
            }

        val body =
            if (willReschedule) {
                "Delivery of $trackingNumber was unsuccessful ($reason). We will try again."
            } else {
                "Delivery of $trackingNumber was unsuccessful ($reason). Please contact support."
            }

        send(
            PushNotification(
                recipientFcmToken = token,
                title = "Delivery attempt unsuccessful",
                body = body,
                data =
                    mapOf(
                        "type" to "DELIVERY_FAILED",
                        "trackingNumber" to trackingNumber,
                        "reason" to reason,
                        "willReschedule" to willReschedule.toString(),
                    ),
            ),
        )
    }

    // ── Shift notifications (to courier) ──────────────────────────────────────

    fun notifyShiftStarted(
        courierFcmToken: String?,
        totalStops: Int,
        scheduledDate: String,
    ) {
        val token =
            courierFcmToken ?: run {
                log.debug("No FCM token for courier — skipping shift started notification")
                return
            }

        send(
            PushNotification(
                recipientFcmToken = token,
                title = "Shift started",
                body = "Your shift for $scheduledDate has $totalStops stops.",
                data =
                    mapOf(
                        "type" to "SHIFT_STARTED",
                        "totalStops" to totalStops.toString(),
                        "scheduledDate" to scheduledDate,
                    ),
            ),
        )
    }

    fun notifyRouteUpdated(
        courierFcmToken: String?,
        totalStops: Int,
        reason: String,
    ) {
        val token =
            courierFcmToken ?: run {
                log.debug("No FCM token for courier — skipping route updated notification")
                return
            }

        send(
            PushNotification(
                recipientFcmToken = token,
                title = "Route updated",
                body = "Your route has been updated — $totalStops stops total.",
                data =
                    mapOf(
                        "type" to "ROUTE_UPDATED",
                        "totalStops" to totalStops.toString(),
                        "reason" to reason,
                    ),
            ),
        )
    }

    fun notifyShiftCompleted(
        courierFcmToken: String?,
        completedStops: Int,
        failedStops: Int,
    ) {
        val token =
            courierFcmToken ?: run {
                log.debug("No FCM token for courier — skipping shift completed notification")
                return
            }

        send(
            PushNotification(
                recipientFcmToken = token,
                title = "Shift complete 🎉",
                body = "Great work! $completedStops delivered, $failedStops failed.",
                data =
                    mapOf(
                        "type" to "SHIFT_COMPLETED",
                        "completedStops" to completedStops.toString(),
                        "failedStops" to failedStops.toString(),
                    ),
            ),
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun send(notification: PushNotification) {
        when (val result = fcmClient.send(notification)) {
            is SendResult.Success -> {
                log.info("Notification sent: messageId=${result.messageId}")
            }

            is SendResult.DryRun -> {
                log.info("Notification logged (dry run): title='${notification.title}'")
            }

            is SendResult.Failure -> {
                log.warn("Notification failed: ${result.reason} (retryable=${result.isRetryable})")
                // In production: push to a dead-letter queue for retry
                // For now, log and continue — a missed notification is not fatal
            }
        }
    }
}
