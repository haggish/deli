package com.deli.gateway.auth

import com.deli.shared.api.request.LoginRequest
import com.deli.shared.api.request.RefreshTokenRequest
import com.deli.shared.api.request.RegisterCourierRequest
import com.deli.shared.api.request.RegisterCustomerRequest
import com.deli.shared.api.response.ApiResponse
import com.deli.shared.api.response.AuthTokenResponse
import com.deli.shared.api.response.UserProfileResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    // ── POST /api/auth/login ──────────────────────────────────────────────────

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): Mono<ApiResponse<AuthTokenResponse>> =
        authService
            .login(request)
            .map { ApiResponse.ok(it) }

    // ── POST /api/auth/refresh ────────────────────────────────────────────────

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): Mono<ApiResponse<AuthTokenResponse>> =
        authService
            .refresh(request.refreshToken)
            .map { ApiResponse.ok(it) }

    // ── POST /api/auth/logout ─────────────────────────────────────────────────

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): Mono<Void> = authService.logout(request.refreshToken)

    // ── POST /api/auth/register/courier ───────────────────────────────────────

    @PostMapping("/register/courier")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerCourier(
        @Valid @RequestBody request: RegisterCourierRequest,
    ): Mono<ApiResponse<UserProfileResponse>> =
        authService
            .registerCourier(request)
            .map { ApiResponse.ok(it) }

    // ── POST /api/auth/register/customer ──────────────────────────────────────

    @PostMapping("/register/customer")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerCustomer(
        @Valid @RequestBody request: RegisterCustomerRequest,
    ): Mono<ApiResponse<UserProfileResponse>> =
        authService
            .registerCustomer(request)
            .map { ApiResponse.ok(it) }
}
