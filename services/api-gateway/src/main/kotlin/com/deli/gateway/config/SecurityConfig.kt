package com.deli.gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            // JWT validation is handled by JwtAuthenticationFilter (GlobalFilter).
            // Spring Security itself is configured to permit all — the filter
            // handles auth logic with full control over the response format.
            .authorizeExchange { exchanges ->
                exchanges.anyExchange().permitAll()
            }
            // Disable Spring Security's default authentication mechanisms —
            // we use our own JWT filter instead
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            // CSRF is not needed for stateless JWT APIs
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.`$2B`, 12)

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config =
            CorsConfiguration().apply {
                // Allow Angular dev server and Capacitor WebView
                allowedOrigins =
                    listOf(
                        "http://localhost:4200",
                        "http://localhost:8100",
                        "capacitor://localhost",
                        "ionic://localhost",
                    )
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("*")
                allowCredentials = true
                maxAge = 3600L
            }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
