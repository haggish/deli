package com.deli.notification.service

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Maps userId → FCM device token, stored in Redis.
 *
 * When a user opens the mobile app and the app receives a new FCM token
 * from Firebase, the mobile app calls PATCH /api/auth/fcm-token on the
 * gateway, which forwards it here to be stored.
 *
 * Key pattern:
 *   fcm:courier:{userId} → FCM token string
 *   fcm:customer:{userId} → FCM token string
 *
 * TTL: 30 days — if a user does not open the app for 30 days the token
 * is considered stale and discarded.
 */
@Service
class FcmTokenRegistry(
    private val redis: StringRedisTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val COURIER_PREFIX = "fcm:courier:"
        private const val CUSTOMER_PREFIX = "fcm:customer:"
        private val TTL = Duration.ofDays(30)
    }

    fun registerCourierToken(
        courierId: String,
        fcmToken: String,
    ) {
        redis.opsForValue().set("$COURIER_PREFIX$courierId", fcmToken, TTL)
        log.debug("Registered FCM token for courier $courierId")
    }

    fun registerCustomerToken(
        customerId: String,
        fcmToken: String,
    ) {
        redis.opsForValue().set("$CUSTOMER_PREFIX$customerId", fcmToken, TTL)
        log.debug("Registered FCM token for customer $customerId")
    }

    fun getCourierToken(courierId: String): String? = redis.opsForValue().get("$COURIER_PREFIX$courierId")

    fun getCustomerToken(customerId: String): String? = redis.opsForValue().get("$CUSTOMER_PREFIX$customerId")

    fun removeCourierToken(courierId: String) {
        redis.delete("$COURIER_PREFIX$courierId")
    }

    fun removeCustomerToken(customerId: String) {
        redis.delete("$CUSTOMER_PREFIX$customerId")
    }
}
