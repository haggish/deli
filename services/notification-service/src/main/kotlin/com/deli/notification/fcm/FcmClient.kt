package com.deli.notification.fcm

import com.deli.notification.domain.PushNotification
import com.deli.notification.domain.SendResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.ObjectMapper

/**
 * Firebase Cloud Messaging client using the FCM HTTP v1 API.
 *
 * In local development (dryRun=true), notifications are logged but not sent.
 * This lets the full pipeline run in CI and locally without Firebase credentials.
 *
 * In production, set deli.fcm.dry-run=false and provide a valid service account
 * key via deli.fcm.service-account-key-json (base64-encoded JSON).
 *
 * The FCM HTTP v1 API requires OAuth2 access tokens derived from the service
 * account key. For simplicity this implementation uses the legacy FCM v1 format
 * with a server key, which is adequate for a courier app with known device tokens.
 */
@Component
class FcmClient(
    private val objectMapper: ObjectMapper,
    @Value("\${deli.fcm.server-key:}") private val serverKey: String,
    @Value("\${deli.fcm.dry-run:true}") private val dryRun: Boolean,
    @Value("\${deli.fcm.project-id:deli-local}") private val projectId: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    private val fcmUrl = "https://fcm.googleapis.com/fcm/send"

    fun send(notification: PushNotification): SendResult {
        if (dryRun) {
            log.info(
                "[DRY RUN] Would send FCM notification: " +
                    "title='${notification.title}' " +
                    "body='${notification.body}' " +
                    "token=${notification.recipientFcmToken.take(20)}...",
            )
            return SendResult.DryRun
        }

        if (serverKey.isBlank()) {
            log.warn("FCM server key not configured — skipping notification send")
            return SendResult.Failure("Server key not configured", isRetryable = false)
        }

        return try {
            val payload = buildPayload(notification)
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set("Authorization", "key=$serverKey")
                }

            val response =
                restTemplate.postForObject(
                    fcmUrl,
                    HttpEntity(payload, headers),
                    Map::class.java,
                )

            val messageId = response?.get("message_id")?.toString()
            if (messageId != null) {
                log.debug("FCM notification sent: messageId=$messageId")
                SendResult.Success(messageId)
            } else {
                val error = response?.get("error")?.toString() ?: "unknown"
                log.warn("FCM returned error: $error")
                SendResult.Failure(error, isRetryable = error == "Unavailable")
            }
        } catch (e: HttpClientErrorException.Unauthorized) {
            log.error("FCM authentication failed — check server key")
            SendResult.Failure("Authentication failed", isRetryable = false)
        } catch (e: Exception) {
            log.error("FCM send failed", e)
            SendResult.Failure(e.message ?: "Unknown error", isRetryable = true)
        }
    }

    private fun buildPayload(notification: PushNotification): Map<String, Any> =
        mapOf(
            "to" to notification.recipientFcmToken,
            "notification" to
                mapOf(
                    "title" to notification.title,
                    "body" to notification.body,
                    "sound" to "default",
                ),
            "data" to notification.data,
            "priority" to "high",
            "content_available" to true,
        )
}
