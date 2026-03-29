package com.deli.shared.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Copies the X-User-Id and X-User-Role headers (injected by the API gateway)
 * into the SLF4J MDC so they appear in every log line for that request.
 *
 * This means you can filter your log aggregator by userId to see exactly
 * what a specific courier or customer did, without adding userId to every
 * individual log.warn() / log.info() call.
 *
 * Include this in any microservice by adding common-api to its dependencies —
 * Spring Boot auto-detects @Component beans.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class MdcLoggingFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val userId = request.getHeader("X-User-Id")
        val role = request.getHeader("X-User-Role")

        try {
            if (userId != null) MDC.put("userId", userId)
            if (role != null) MDC.put("userRole", role)

            filterChain.doFilter(request, response)
        } finally {
            // Always clear MDC — threads are pooled and reused across requests
            MDC.remove("userId")
            MDC.remove("userRole")
        }
    }
}
