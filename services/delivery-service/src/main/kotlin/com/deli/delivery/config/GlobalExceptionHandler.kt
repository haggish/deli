package com.deli.delivery.config

import com.deli.shared.api.response.ApiError
import com.deli.shared.api.response.ApiResponse
import com.deli.shared.domain.model.DeliException
import com.deli.shared.domain.model.StopAlreadyCompletedException
import com.deli.shared.domain.model.StopNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(DeliException::class)
    fun handleDomainException(ex: DeliException): ResponseEntity<ApiResponse<Nothing>> {
        val status =
            when (ex) {
                is StopNotFoundException -> HttpStatus.NOT_FOUND
                is StopAlreadyCompletedException -> HttpStatus.CONFLICT
                else -> HttpStatus.BAD_REQUEST
            }
        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(ApiError(ex::class.simpleName ?: "DomainError", ex.message ?: "")))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val details = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid") }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ApiError("VALIDATION_ERROR", "Validation failed", details)))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("Unhandled exception", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ApiError("INTERNAL_ERROR", "An unexpected error occurred")))
    }
}
