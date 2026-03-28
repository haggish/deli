package com.deli.location.service

import com.deli.location.domain.CourierPosition
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class CourierPositionCache(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${deli.location.courier-ttl-seconds:60}") private val ttlSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val KEY_PREFIX = "courier:position:"
    }

    fun update(position: CourierPosition) {
        val key = "$KEY_PREFIX${position.courierId}"
        val value = objectMapper.writeValueAsString(position)
        redis.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds))
        log.debug("Updated position cache for courier ${position.courierId}")
    }

    fun get(courierId: String): CourierPosition? {
        val key = "$KEY_PREFIX$courierId"
        val value = redis.opsForValue().get(key) ?: return null
        return try {
            objectMapper.readValue(value, CourierPosition::class.java)
        } catch (e: Exception) {
            log.warn("Failed to deserialize position for courier $courierId", e)
            null
        }
    }

    fun getAll(courierIds: List<String>): Map<String, CourierPosition> {
        if (courierIds.isEmpty()) return emptyMap()

        val keys = courierIds.map { "$KEY_PREFIX$it" }
        val values = redis.opsForValue().multiGet(keys) ?: return emptyMap()

        return courierIds
            .zip(values)
            .filter { (_, value) -> value != null }
            .mapNotNull { (courierId, value) ->
                try {
                    courierId to objectMapper.readValue(value, CourierPosition::class.java)
                } catch (e: Exception) {
                    null
                }
            }.toMap()
    }

    fun remove(courierId: String) {
        redis.delete("$KEY_PREFIX$courierId")
    }

    fun isOnline(courierId: String): Boolean = redis.hasKey("$KEY_PREFIX$courierId") == true
}
