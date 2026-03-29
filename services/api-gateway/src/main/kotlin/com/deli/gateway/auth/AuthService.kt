package com.deli.gateway.auth

import com.deli.shared.api.request.LoginRequest
import com.deli.shared.api.request.RegisterCourierRequest
import com.deli.shared.api.request.RegisterCustomerRequest
import com.deli.shared.api.response.AuthTokenResponse
import com.deli.shared.api.response.UserProfileResponse
import com.deli.shared.domain.model.UserRole
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
    private val userRepository: GatewayUserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenService: JwtTokenService,
    private val passwordEncoder: PasswordEncoder,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ── Login ─────────────────────────────────────────────────────────────────

    fun login(request: LoginRequest): Mono<AuthTokenResponse> =
        userRepository
            .findByEmailAndIsActiveTrue(request.email)
            .filter { user -> passwordEncoder.matches(request.password, user.passwordHash) }
            .switchIfEmpty(Mono.error(AuthException.InvalidCredentials()))
            .flatMap { user ->
                val accessToken =
                    jwtTokenService.issueAccessToken(
                        userId = user.id.toString(),
                        email = user.email,
                        role = user.role(),
                    )
                val refreshToken = jwtTokenService.issueRefreshToken(user.id.toString())

                // Store hashed refresh token
                val tokenHash = sha256(refreshToken)
                val expiresAt =
                    jwtTokenService.expiresAt(refreshToken)
                        ?: Instant.now().plusSeconds(30L * 24 * 60 * 60)

                r2dbcEntityTemplate
                    .insert(RefreshToken::class.java)
                    .using(
                        RefreshToken(
                            userId = user.id,
                            tokenHash = tokenHash,
                            expiresAt = expiresAt,
                        ),
                    ).thenReturn(
                        AuthTokenResponse(
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            expiresInSeconds = jwtTokenService.run { 60L * 60 },
                        ),
                    )
            }.doOnError { log.warn("Login failed for ${request.email}: ${it.message}") }

    // ── Token refresh ─────────────────────────────────────────────────────────

    fun refresh(refreshToken: String): Mono<AuthTokenResponse> {
        if (!jwtTokenService.isRefreshToken(refreshToken)) {
            return Mono.error(AuthException.InvalidToken())
        }

        val validationResult = jwtTokenService.validateToken(refreshToken)
        if (validationResult !is TokenValidationResult.Valid) {
            return Mono.error(AuthException.TokenExpired())
        }

        val tokenHash = sha256(refreshToken)
        val userId =
            jwtTokenService.extractUserId(refreshToken)
                ?: return Mono.error(AuthException.InvalidToken())

        return refreshTokenRepository
            .findByTokenHashAndRevokedFalse(tokenHash)
            .switchIfEmpty(Mono.error(AuthException.InvalidToken()))
            .flatMap { storedToken ->
                if (storedToken.expiresAt.isBefore(Instant.now())) {
                    return@flatMap Mono.error(AuthException.TokenExpired())
                }

                userRepository
                    .findById(UUID.fromString(userId))
                    .switchIfEmpty(Mono.error(AuthException.InvalidToken()))
                    .flatMap { user ->
                        // Revoke old refresh token (rotation)
                        val revoked = storedToken.copy(revoked = true)

                        val newAccessToken =
                            jwtTokenService.issueAccessToken(
                                userId = user.id.toString(),
                                email = user.email,
                                role = user.role(),
                            )
                        val newRefreshToken = jwtTokenService.issueRefreshToken(user.id.toString())
                        val newHash = sha256(newRefreshToken)
                        val newExpiry =
                            jwtTokenService.expiresAt(newRefreshToken)
                                ?: Instant.now().plusSeconds(30L * 24 * 60 * 60)

                        refreshTokenRepository
                            .save(revoked)
                            .then(
                                refreshTokenRepository.save(
                                    RefreshToken(
                                        userId = user.id,
                                        tokenHash = newHash,
                                        expiresAt = newExpiry,
                                    ),
                                ),
                            ).thenReturn(
                                AuthTokenResponse(
                                    accessToken = newAccessToken,
                                    refreshToken = newRefreshToken,
                                    expiresInSeconds = 60L * 60,
                                ),
                            )
                    }
            }
    }

    // ── Registration ──────────────────────────────────────────────────────────

    fun registerCourier(request: RegisterCourierRequest): Mono<UserProfileResponse> =
        checkEmailAvailable(request.email)
            .flatMap {
                userRepository.save(
                    GatewayUser(
                        email = request.email,
                        passwordHash = passwordEncoder.encode(request.password)!!,
                        role = UserRole.COURIER.name,
                        firstName = request.firstName,
                        lastName = request.lastName,
                        phoneNumber = request.phoneNumber,
                    ),
                )
            }.map { user ->
                UserProfileResponse(
                    id = user.id.toString(),
                    email = user.email,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    role = user.role,
                    phoneNumber = user.phoneNumber,
                )
            }

    fun registerCustomer(request: RegisterCustomerRequest): Mono<UserProfileResponse> =
        checkEmailAvailable(request.email)
            .flatMap {
                userRepository.save(
                    GatewayUser(
                        email = request.email,
                        passwordHash = passwordEncoder.encode(request.password)!!,
                        role = UserRole.CUSTOMER.name,
                        firstName = request.firstName,
                        lastName = request.lastName,
                        phoneNumber = request.phoneNumber,
                    ),
                )
            }.map { user ->
                UserProfileResponse(
                    id = user.id.toString(),
                    email = user.email,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    role = user.role,
                    phoneNumber = user.phoneNumber,
                )
            }

    // ── Logout ────────────────────────────────────────────────────────────────

    fun logout(refreshToken: String): Mono<Void> {
        val tokenHash = sha256(refreshToken)
        return refreshTokenRepository
            .findByTokenHashAndRevokedFalse(tokenHash)
            .flatMap { stored ->
                refreshTokenRepository.save(stored.copy(revoked = true))
            }.then()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun checkEmailAvailable(email: String): Mono<Boolean> =
        userRepository
            .existsByEmail(email)
            .flatMap { exists ->
                if (exists) {
                    Mono.error(AuthException.EmailAlreadyExists(email))
                } else {
                    Mono.just(true)
                }
            }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

// ── Auth exceptions ───────────────────────────────────────────────────────────

sealed class AuthException(
    message: String,
) : RuntimeException(message) {
    class InvalidCredentials : AuthException("Invalid email or password")

    class InvalidToken : AuthException("Token is invalid or has been revoked")

    class TokenExpired : AuthException("Token has expired")

    class EmailAlreadyExists(
        email: String,
    ) : AuthException("Email already registered: $email")

    class AccountDisabled : AuthException("Account is disabled")
}
