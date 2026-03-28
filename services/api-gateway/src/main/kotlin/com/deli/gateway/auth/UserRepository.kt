package com.deli.gateway.auth

import com.deli.shared.domain.model.UserRole
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

// ── User entity ───────────────────────────────────────────────────────────────
// The gateway owns user authentication. Individual microservices reference
// userId as a foreign key but do not store passwords.

@Table("gateway_users")
data class GatewayUser(
    @Id
    val id: UUID = UUID.randomUUID(),
    val email: String,
    @Column("password_hash")
    val passwordHash: String,
    val role: String, // stored as string — maps to UserRole enum
    @Column("first_name")
    val firstName: String,
    @Column("last_name")
    val lastName: String,
    @Column("phone_number")
    val phoneNumber: String,
    @Column("is_active")
    val isActive: Boolean = true,
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    @Column("last_login_at")
    val lastLoginAt: Instant? = null,
) {
    fun role(): UserRole = UserRole.valueOf(role)

    fun fullName(): String = "$firstName $lastName"
}

// ── Refresh token entity ──────────────────────────────────────────────────────

@Table("refresh_tokens")
data class RefreshToken(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("user_id")
    val userId: UUID,
    @Column("token_hash")
    val tokenHash: String,
    @Column("expires_at")
    val expiresAt: Instant,
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    @Column("revoked")
    val revoked: Boolean = false,
)

// ── Repositories ──────────────────────────────────────────────────────────────

@Repository
interface GatewayUserRepository : ReactiveCrudRepository<GatewayUser, UUID> {
    fun findByEmail(email: String): Mono<GatewayUser>

    fun findByEmailAndIsActiveTrue(email: String): Mono<GatewayUser>

    fun existsByEmail(email: String): Mono<Boolean>
}

@Repository
interface RefreshTokenRepository : ReactiveCrudRepository<RefreshToken, UUID> {
    fun findByTokenHashAndRevokedFalse(tokenHash: String): Mono<RefreshToken>

    fun findAllByUserId(userId: UUID): reactor.core.publisher.Flux<RefreshToken>
}
