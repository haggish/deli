package com.deli.location.config

import com.deli.location.controller.LocationWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.HandshakeInterceptor

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val locationWebSocketHandler: LocationWebSocketHandler,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            .addHandler(locationWebSocketHandler, "/ws/location")
            // Extract X-User-Id from the gateway-injected header and store in session attributes
            .addInterceptors(GatewayHeaderInterceptor())
            // Allow all origins — the gateway handles CORS
            .setAllowedOrigins("*")
    }
}

/**
 * Extracts the X-User-Id header (injected by the API gateway's JWT filter)
 * and stores it as a session attribute so the WebSocket handler can access it.
 *
 * This runs during the HTTP handshake, before the connection upgrades to WebSocket.
 * If the header is missing, the handshake is rejected with a 401.
 */
class GatewayHeaderInterceptor : HandshakeInterceptor {
    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val userId = request.headers.getFirst("X-User-Id")
        val role = request.headers.getFirst("X-User-Role")

        if (userId == null) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED)
            return false
        }

        attributes["userId"] = userId
        attributes["role"] = role ?: ""
        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?,
    ) {
        // Nothing to do after handshake
    }
}
