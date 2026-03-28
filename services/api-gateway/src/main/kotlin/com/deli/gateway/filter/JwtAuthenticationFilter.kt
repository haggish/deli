package com.deli.gateway.filter

import com.deli.gateway.auth.JwtTokenService
import com.deli.gateway.auth.TokenValidationResult
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationFilter(
    private val jwtTokenService: JwtTokenService,
) : GlobalFilter,
    Ordered {
    private val log = LoggerFactory.getLogger(javaClass)

    // Run before routing filters (lower number = higher priority)
    override fun getOrder(): Int = -100

    // Paths that do not require authentication
    private val publicPaths =
        setOf(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/register/courier",
            "/api/auth/register/customer",
            "/actuator/health",
            "/actuator/health/liveness",
            "/actuator/health/readiness",
            "/actuator/info",
            "/actuator/prometheus",
        )

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val path = exchange.request.uri.path

        // Allow public paths through without authentication
        if (publicPaths.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Missing or malformed Authorization header for path: $path")
            return unauthorized(exchange)
        }

        val token = authHeader.removePrefix("Bearer ")

        return when (val result = jwtTokenService.validateToken(token)) {
            is TokenValidationResult.Valid -> {
                // Reject refresh tokens used as access tokens
                if (jwtTokenService.isRefreshToken(token)) {
                    log.debug("Refresh token used as access token rejected for path: $path")
                    return unauthorized(exchange)
                }

                val userId = result.claims.subject
                val role = result.claims["role"] as? String ?: ""
                val email = result.claims["email"] as? String ?: ""

                // Forward user identity to downstream services as headers
                // Services read these headers to know who is making the request
                val mutatedRequest =
                    exchange.request
                        .mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role)
                        .header("X-User-Email", email)
                        .build()

                chain.filter(exchange.mutate().request(mutatedRequest).build())
            }

            is TokenValidationResult.Expired -> {
                log.debug("Expired token for path: $path")
                unauthorized(exchange, "Token expired")
            }

            is TokenValidationResult.Invalid -> {
                log.debug("Invalid token for path: $path")
                unauthorized(exchange)
            }
        }
    }

    private fun unauthorized(
        exchange: ServerWebExchange,
        message: String = "Unauthorized",
    ): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.set("Content-Type", "application/json")
        val body = """{"success":false,"error":{"code":"UNAUTHORIZED","message":"$message"}}"""
        val buffer = response.bufferFactory().wrap(body.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }
}
