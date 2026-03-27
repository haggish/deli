package com.deli.shared.domain.model

import com.deli.shared.domain.valueobject.CourierId
import com.deli.shared.domain.valueobject.PackageId
import com.deli.shared.domain.valueobject.ShiftId
import com.deli.shared.domain.valueobject.StopId
import com.deli.shared.domain.valueobject.TrackingNumber

// ── Base exception ────────────────────────────────────────────────────────────

sealed class DeliException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

// ── Not found exceptions ──────────────────────────────────────────────────────

class CourierNotFoundException(
    id: CourierId,
) : DeliException("Courier not found: $id")

class PackageNotFoundException(
    id: PackageId,
) : DeliException("Package not found: $id")

class PackageNotFoundByTrackingException(
    tracking: TrackingNumber,
) : DeliException("Package not found for tracking number: $tracking")

class StopNotFoundException(
    id: StopId,
) : DeliException("Stop not found: $id")

class ShiftNotFoundException(
    id: ShiftId,
) : DeliException("Shift not found: $id")

// ── State machine violations ──────────────────────────────────────────────────

class InvalidDeliveryStateTransitionException(
    from: DeliveryStatus,
    to: DeliveryStatus,
    packageId: PackageId,
) : DeliException("Cannot transition package $packageId from $from to $to")

class StopAlreadyCompletedException(
    stopId: StopId,
) : DeliException("Stop $stopId is already completed and cannot be modified")

class ShiftNotActiveException(
    shiftId: ShiftId,
) : DeliException("Shift $shiftId is not active — cannot perform delivery operations")

// ── Business rule violations ──────────────────────────────────────────────────

class CourierNotAssignedToShiftException(
    courierId: CourierId,
    shiftId: ShiftId,
) : DeliException("Courier $courierId is not assigned to shift $shiftId")

class ProofOfDeliveryRequiredException(
    stopId: StopId,
) : DeliException("A proof photo or signature is required for stop $stopId")

class DuplicateTrackingNumberException(
    tracking: TrackingNumber,
) : DeliException("Tracking number already exists: $tracking")

// ── Infrastructure exceptions ─────────────────────────────────────────────────

class FileUploadException(
    message: String,
    cause: Throwable? = null,
) : DeliException("File upload failed: $message", cause)

class ExternalServiceException(
    service: String,
    message: String,
    cause: Throwable? = null,
) : DeliException("External service [$service] error: $message", cause)
