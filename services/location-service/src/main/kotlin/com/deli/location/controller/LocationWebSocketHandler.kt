package com.deli.location.controller

import com.deli.location.service.LocationService
import com.deli.shared.api.request.LocationPingRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket handler for inbound GPS pings from courier devices.
 *
 * Protocol:
 * - Client connects to ws://host/ws/location with Authorization header
 * - Client sends JSON LocationPingRequest messages every 5–15 seconds
 * - Server responds with {"status":"ok"} or {"status":"error","message":"..."}
 * - Connection drops on shift completion — client reconnects for next shift
 *
 * The JWT is validated by the gateway before the WebSocket upgrade.
 * X-User-Id and X-User-Role headers are injected by the gateway and
 * available on the WebSocket session attributes.
 */
@Component
class LocationWebSocketHandler(
    private val locationService: LocationService,
    private val objectMapper: ObjectMapper,
) : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    // Track active courier sessions — courierId → session
    private val activeSessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val courierId = session.attributes["userId"] as? String
        if (courierId == null) {
            log.warn("WebSocket connection rejected — no userId in session attributes")
            session.close(CloseStatus.POLICY_VIOLATION)
            return
        }
        activeSessions[courierId] = session
        log.info("Courier $courierId connected via WebSocket (session ${session.id})")
        session.sendMessage(TextMessage("""{"status":"connected","courierId":"$courierId"}"""))
    }

    override fun handleTextMessage(
        session: WebSocketSession,
        message: TextMessage,
    ) {
        val courierId =
            session.attributes["userId"] as? String ?: run {
                session.close(CloseStatus.POLICY_VIOLATION)
                return
            }

        try {
            val request = objectMapper.readValue(message.payload, LocationPingRequest::class.java)
            val shiftId = UUID.fromString(request.shiftId)

            locationService.processPing(
                courierId = UUID.fromString(courierId),
                shiftId = shiftId,
                request = request,
            )

            session.sendMessage(TextMessage("""{"status":"ok"}"""))
        } catch (e: Exception) {
            log.error("Failed to process ping from courier $courierId: ${e.message}", e)
            session.sendMessage(
                TextMessage("""{"status":"error","message":"${e.message?.replace("\"", "'")}"}"""),
            )
        }
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        status: CloseStatus,
    ) {
        val courierId = session.attributes["userId"] as? String
        if (courierId != null) {
            activeSessions.remove(courierId)
            log.info("Courier $courierId disconnected (${status.code}: ${status.reason})")
        }
    }

    override fun handleTransportError(
        session: WebSocketSession,
        exception: Throwable,
    ) {
        val courierId = session.attributes["userId"] as? String ?: "unknown"
        log.error("WebSocket transport error for courier $courierId", exception)
    }

    fun getActiveSessionCount(): Int = activeSessions.size

    fun isConnected(courierId: String): Boolean = activeSessions.containsKey(courierId)
}
