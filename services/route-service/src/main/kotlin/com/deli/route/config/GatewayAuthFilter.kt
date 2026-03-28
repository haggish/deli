package com.deli.route.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Microservice-side authentication filter.
 *
 * The API gateway validates JWTs and injects trusted headers before
 * forwarding requests to microservices. This filter reads those headers
 * and establishes a SecurityContext — no JWT library needed here.
 *
 * IMPORTANT: This filter must only be reachable through the API gateway.
 * In production, network policies (Kubernetes NetworkPolicy) prevent direct
 * access to microservice ports from outside the cluster.
 */
@Component
class GatewayAuthFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val userId = request.getHeader("X-User-Id")
        val role = request.getHeader("X-User-Role")

        if (userId != null && role != null) {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
            val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
            SecurityContextHolder.getContext().authentication = auth
        }

        filterChain.doFilter(request, response)
    }
}
