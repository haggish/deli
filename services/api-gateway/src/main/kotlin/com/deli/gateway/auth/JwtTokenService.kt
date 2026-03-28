package com.deli.gateway.auth

import com.deli.shared.domain.model.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtTokenService(
    @Value("\${deli.jwt.secret}") secret: String,
    @Value("\${deli.jwt.expiry-minutes:60}") private val expiryMinutes: Long,
    @Value("\${deli.jwt.refresh-expiry-days:30}") private val refreshExpiryDays: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Derive a 256-bit HMAC-SHA key from the configured secret string
    private val signingKey: SecretKey =
        Keys.hmacShaKeyFor(
            secret.toByteArray(Charsets.UTF_8).let { bytes ->
                // Pad or truncate to exactly 32 bytes (256 bits) for HS256
                ByteArray(32) { i -> bytes.getOrElse(i) { 0 } }
            },
        )

    // ── Token issuance ────────────────────────────────────────────────────────

    fun issueAccessToken(
        userId: String,
        email: String,
        role: UserRole,
    ): String {
        val now = Instant.now()
        return Jwts
            .builder()
            .subject(userId)
            .claim("email", email)
            .claim("role", role.name)
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expiryMinutes * 60)))
            .signWith(signingKey)
            .compact()
    }

    fun issueRefreshToken(userId: String): String {
        val now = Instant.now()
        return Jwts
            .builder()
            .subject(userId)
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(refreshExpiryDays * 24 * 60 * 60)))
            .signWith(signingKey)
            .compact()
    }

    // ── Token validation ──────────────────────────────────────────────────────

    fun validateToken(token: String): TokenValidationResult =
        try {
            val claims = parseClaims(token)
            TokenValidationResult.Valid(claims)
        } catch (e: ExpiredJwtException) {
            log.debug("JWT token expired")
            TokenValidationResult.Expired
        } catch (e: JwtException) {
            log.debug("JWT token invalid: ${e.message}")
            TokenValidationResult.Invalid
        } catch (e: IllegalArgumentException) {
            log.debug("JWT token blank or malformed")
            TokenValidationResult.Invalid
        }

    fun extractUserId(token: String): String? = runCatching { parseClaims(token).subject }.getOrNull()

    fun extractRole(token: String): UserRole? =
        runCatching {
            val roleName = parseClaims(token)["role"] as? String ?: return null
            UserRole.valueOf(roleName)
        }.getOrNull()

    fun extractEmail(token: String): String? = runCatching { parseClaims(token)["email"] as? String }.getOrNull()

    fun isRefreshToken(token: String): Boolean =
        runCatching { parseClaims(token)["type"] == "refresh" }.getOrElse { false }

    fun isAccessToken(token: String): Boolean =
        runCatching { parseClaims(token)["type"] == "access" }.getOrElse { false }

    fun expiresAt(token: String): Instant? = runCatching { parseClaims(token).expiration.toInstant() }.getOrNull()

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun parseClaims(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
}

sealed class TokenValidationResult {
    data class Valid(
        val claims: Claims,
    ) : TokenValidationResult()

    data object Expired : TokenValidationResult()

    data object Invalid : TokenValidationResult()
}
