package com.deli.delivery.controller

import com.deli.delivery.domain.DeliveryRecord
import com.deli.delivery.service.DeliveryService
import com.deli.shared.api.request.ConfirmDeliveryRequest
import com.deli.shared.api.request.ReportFailedDeliveryRequest
import com.deli.shared.api.request.RequestPhotoUploadUrlRequest
import com.deli.shared.api.response.ApiResponse
import com.deli.shared.api.response.DeliveryConfirmationResponse
import com.deli.shared.api.response.PhotoUploadUrlResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/deliveries")
class DeliveryController(
    private val deliveryService: DeliveryService,
) {
    // ── GET /api/deliveries/stops/{stopId} ─────────────────────────────────────

    @GetMapping("/stops/{stopId}")
    fun getDelivery(
        @PathVariable stopId: UUID,
    ): ApiResponse<DeliveryConfirmationResponse> {
        val record = deliveryService.getByStopId(stopId)
        return ApiResponse.ok(record.toResponse())
    }

    // ── POST /api/deliveries/stops/{stopId}/confirm ────────────────────────────

    @PostMapping("/stops/{stopId}/confirm")
    fun confirmDelivery(
        @PathVariable stopId: UUID,
        @Valid @RequestBody request: ConfirmDeliveryRequest,
    ): ApiResponse<DeliveryConfirmationResponse> {
        val record = deliveryService.confirmDelivery(stopId, request)
        return ApiResponse.ok(record.toResponse())
    }

    // ── POST /api/deliveries/stops/{stopId}/fail ───────────────────────────────

    @PostMapping("/stops/{stopId}/fail")
    fun reportFailure(
        @PathVariable stopId: UUID,
        @Valid @RequestBody request: ReportFailedDeliveryRequest,
    ): ApiResponse<DeliveryConfirmationResponse> {
        val record = deliveryService.reportFailure(stopId, request)
        return ApiResponse.ok(record.toResponse())
    }

    // ── POST /api/deliveries/upload-url/photo ──────────────────────────────────

    @PostMapping("/upload-url/photo")
    fun getPhotoUploadUrl(
        @Valid @RequestBody request: RequestPhotoUploadUrlRequest,
    ): ApiResponse<PhotoUploadUrlResponse> {
        val result = deliveryService.getPhotoUploadUrl(request)
        return ApiResponse.ok(
            PhotoUploadUrlResponse(
                uploadUrl = result.uploadUrl,
                fileKey = result.fileKey,
                expiresAt = result.expiresAt,
            ),
        )
    }

    // ── POST /api/deliveries/upload-url/signature/{stopId} ────────────────────

    @PostMapping("/upload-url/signature/{stopId}")
    fun getSignatureUploadUrl(
        @PathVariable stopId: UUID,
    ): ApiResponse<PhotoUploadUrlResponse> {
        val result = deliveryService.getSignatureUploadUrl(stopId)
        return ApiResponse.ok(
            PhotoUploadUrlResponse(
                uploadUrl = result.uploadUrl,
                fileKey = result.fileKey,
                expiresAt = result.expiresAt,
            ),
        )
    }

    // ── PATCH /api/deliveries/stops/{stopId}/photo-uploaded ────────────────────

    @PatchMapping("/stops/{stopId}/photo-uploaded")
    fun recordPhotoUploaded(
        @PathVariable stopId: UUID,
        @RequestBody body: Map<String, String>,
    ): ApiResponse<DeliveryConfirmationResponse> {
        val fileKey =
            body["fileKey"] ?: return ApiResponse.error(
                com.deli.shared.api.response
                    .ApiError("MISSING_FIELD", "fileKey is required"),
            )
        val record = deliveryService.recordPhotoUploaded(stopId, fileKey)
        return ApiResponse.ok(record.toResponse())
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private fun DeliveryRecord.toResponse() =
        DeliveryConfirmationResponse(
            stopId = stopId.toString(),
            packageId = packageId.toString(),
            trackingNumber = trackingNumber,
            status = status,
            placement = placement,
            proofPhotoUploadUrl = null, // only populated when explicitly requested
            signatureUploadUrl = null,
            confirmedAt = confirmedAt ?: updatedAt,
        )
}
