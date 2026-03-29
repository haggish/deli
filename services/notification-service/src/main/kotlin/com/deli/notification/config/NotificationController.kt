package com.deli.notification.config

import com.deli.notification.service.FcmTokenRegistry
import com.deli.shared.api.response.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val tokenRegistry: FcmTokenRegistry,
) {
    /**
     * Called by the mobile app whenever Firebase issues a new FCM token.
     * Also called on first launch after login.
     *
     * The role comes from the gateway-injected X-User-Role header so the
     * service knows whether to store the token as a courier or customer token.
     */
    @PatchMapping("/fcm-token")
    fun registerFcmToken(
        @RequestHeader("X-User-Id") userId: String,
        @RequestHeader("X-User-Role") role: String,
        @RequestBody body: Map<String, String>,
    ): ApiResponse<Nothing?> {
        val token = body["fcmToken"]
            ?: return ApiResponse.error(
                com.deli.shared.api.response.ApiError("MISSING_FIELD", "fcmToken is required"),
            )

        when (role.uppercase()) {
            "COURIER" -> tokenRegistry.registerCourierToken(userId, token)
            "CUSTOMER" -> tokenRegistry.registerCustomerToken(userId, token)
            else -> return ApiResponse.error(
                com.deli.shared.api.response.ApiError("INVALID_ROLE", "Unknown role: $role"),
            )
        }

        return ApiResponse.ok(null)
    }

    /**
     * Called when a user logs out — removes their FCM token so they
     * stop receiving push notifications on this device.
     */
    @DeleteMapping("/fcm-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeFcmToken(
        @RequestHeader("X-User-Id") userId: String,
        @RequestHeader("X-User-Role") role: String,
    ) {
        when (role.uppercase()) {
            "COURIER" -> tokenRegistry.removeCourierToken(userId)
            "CUSTOMER" -> tokenRegistry.removeCustomerToken(userId)
        }
    }
}
