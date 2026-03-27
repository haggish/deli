package com.deli.shared.domain.model

// ── User roles ────────────────────────────────────────────────────────────────

enum class UserRole {
    /** Delivery personnel — uses the courier UI */
    COURIER,

    /** End customer — uses the customer UI */
    CUSTOMER,

    /** Internal dispatcher / operations staff */
    DISPATCHER,
}

// ── Delivery lifecycle states ─────────────────────────────────────────────────
// The valid state machine transitions are:
//
//  PENDING ──► ASSIGNED ──► IN_TRANSIT ──► DELIVERED
//                │               │
//                └───────────────┴──► FAILED ──► RESCHEDULED
//                                │
//                                └──► RETURNED

enum class DeliveryStatus {
    /** Package registered, not yet assigned to a courier */
    PENDING,

    /** Assigned to a courier's shift, not yet picked up */
    ASSIGNED,

    /** Courier has picked up the package and is en route */
    IN_TRANSIT,

    /** Successfully delivered to the customer or a safe location */
    DELIVERED,

    /** Delivery attempted but failed (no access, no one home, etc.) */
    FAILED,

    /** Failed delivery rescheduled for another slot */
    RESCHEDULED,

    /** Package returned to the depot after exhausting delivery attempts */
    RETURNED,
}

// ── Where a package was left when delivered ────────────────────────────────────

enum class DeliveryPlacement {
    /** Handed directly to the customer */
    HANDED_TO_CUSTOMER,

    /** Left at the front door */
    FRONT_DOOR,

    /** Left at building reception */
    RECEPTION,

    /** Left with a neighbour */
    NEIGHBOUR,

    /** Left in a parcel locker */
    PARCEL_LOCKER,

    /** Other — description captured in free-text note */
    OTHER,
}

// ── Reason a delivery attempt failed ─────────────────────────────────────────

enum class FailureReason {
    /** No one answered the door */
    NO_ANSWER,

    /** Address not found or does not exist */
    ADDRESS_NOT_FOUND,

    /** Could not access the building (no buzzer code, locked gate, etc.) */
    NO_ACCESS,

    /** Customer refused the delivery */
    REFUSED_BY_CUSTOMER,

    /** Package damaged in transit */
    DAMAGED,

    /** Incorrect address on label */
    WRONG_ADDRESS,

    /** Other — captured in free-text note */
    OTHER,
}

// ── Stop ordering within a courier's shift ────────────────────────────────────

enum class StopStatus {
    /** Waiting in the queue */
    PENDING,

    /** Currently active — courier is on the way */
    IN_PROGRESS,

    /** Delivery completed (success or failure) */
    COMPLETED,

    /** Skipped (re-queued or cancelled) */
    SKIPPED,
}

// ── Package flags ─────────────────────────────────────────────────────────────

enum class PackageFlag {
    FRAGILE,
    KEEP_UPRIGHT,
    KEEP_REFRIGERATED,
    HAZARDOUS,
    REQUIRES_SIGNATURE,
    CASH_ON_DELIVERY,
    PROOF_OF_AGE_REQUIRED,
}

// ── Shift status ──────────────────────────────────────────────────────────────

enum class ShiftStatus {
    SCHEDULED,
    ACTIVE,
    COMPLETED,
    CANCELLED,
}
