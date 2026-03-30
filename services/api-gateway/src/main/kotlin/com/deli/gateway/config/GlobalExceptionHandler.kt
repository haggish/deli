package com.deli.gateway.config

import com.deli.gateway.auth.AuthException
import com.deli.shared.api.response.ApiError
import com.deli.shared.api.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebExceptionHandler
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper

@Component
@Order(-2)
class GlobalExceptionHandler(
    private val objectMapper: ObjectMapper,
) : WebExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        exchange: ServerWebExchange,
        ex: Throwable,
    ): Mono<Void> {
        val (status, error) =
            when (ex) {
                is AuthException.InvalidCredentials -> {
                    HttpStatus.UNAUTHORIZED to ApiError("INVALID_CREDENTIALS", ex.message ?: "Invalid credentials")
                }

                is AuthException.InvalidToken -> {
                    HttpStatus.UNAUTHORIZED to ApiError("INVALID_TOKEN", ex.message ?: "Invalid token")
                }

                is AuthException.TokenExpired -> {
                    HttpStatus.UNAUTHORIZED to ApiError("TOKEN_EXPIRED", "Token has expired")
                }

                is AuthException.EmailAlreadyExists -> {
                    HttpStatus.CONFLICT to ApiError("EMAIL_EXISTS", ex.message ?: "Email already registered")
                }

                is AuthException.AccountDisabled -> {
                    HttpStatus.FORBIDDEN to ApiError("ACCOUNT_DISABLED", ex.message ?: "Account disabled")
                }

                is WebExchangeBindException -> {
                    val details =
                        ex.bindingResult.fieldErrors
                            .associate { it.field to (it.defaultMessage ?: "Invalid value") }
                    HttpStatus.BAD_REQUEST to
                        ApiError(
                            code = "VALIDATION_ERROR",
                            message = "Request validation failed",
                            details = details,
                        )
                }

                else -> {
                    log.error("Unhandled exception", ex)
                    HttpStatus.INTERNAL_SERVER_ERROR to
                        ApiError(
                            "INTERNAL_ERROR",
                            "An unexpected error occurred",
                        )
                }
            }

        val response = exchange.response
        response.statusCode = status
        response.headers.contentType = MediaType.APPLICATION_JSON

        val body = objectMapper.writeValueAsBytes(ApiResponse.error<Nothing>(error))
        val buffer = response.bufferFactory().wrap(body)
        return response.writeWith(Mono.just(buffer))
    }
}
