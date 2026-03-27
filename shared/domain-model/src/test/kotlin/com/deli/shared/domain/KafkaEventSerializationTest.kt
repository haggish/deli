package com.deli.shared.domain

import com.deli.shared.domain.events.DeliveryConfirmedEvent
import com.deli.shared.domain.events.DeliveryFailedEvent
import com.deli.shared.domain.events.EventEnvelope
import com.deli.shared.domain.events.EventTypes
import com.deli.shared.domain.events.LocationUpdatedEvent
import com.deli.shared.domain.events.StopAssignedEvent
import com.deli.shared.domain.model.DeliveryPlacement
import com.deli.shared.domain.model.DeliveryStatus
import com.deli.shared.domain.model.FailureReason
import com.deli.shared.domain.valueobject.Coordinates
import com.deli.shared.domain.valueobject.CourierId
import com.deli.shared.domain.valueobject.CustomerId
import com.deli.shared.domain.valueobject.PackageId
import com.deli.shared.domain.valueobject.ShiftId
import com.deli.shared.domain.valueobject.StopId
import com.deli.shared.domain.valueobject.TrackingNumber
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

class KafkaEventSerializationTest :
    DescribeSpec({

        val json =
            Json {
                prettyPrint = false
                ignoreUnknownKeys = true // forward-compatible: new fields in future versions are ignored
                encodeDefaults = true
            }

        val courierId = CourierId.new()
        val customerId = CustomerId.new()
        val packageId = PackageId.new()
        val stopId = StopId.new()
        val shiftId = ShiftId.new()
        val now = Instant.parse("2025-04-01T09:30:00Z")

        describe("LocationUpdatedEvent") {

            it("round-trips through JSON without data loss") {
                val event =
                    LocationUpdatedEvent(
                        courierId = courierId,
                        shiftId = shiftId,
                        coordinates = Coordinates(52.5200, 13.4050),
                        accuracyMetres = 5.0,
                        speedKmh = 32.5,
                        headingDegrees = 270.0,
                        recordedAt = now,
                    )
                val envelope =
                    EventEnvelope(
                        eventType = EventTypes.LOCATION_UPDATED,
                        payload = event,
                    )

                val serialized = json.encodeToString(envelope)
                val deserialized = json.decodeFromString<EventEnvelope<LocationUpdatedEvent>>(serialized)

                deserialized.payload.courierId shouldBe event.courierId
                deserialized.payload.coordinates.latitude shouldBe 52.5200
                deserialized.payload.coordinates.longitude shouldBe 13.4050
                deserialized.payload.speedKmh shouldBe 32.5
                deserialized.payload.recordedAt shouldBe now
                deserialized.eventId shouldNotBe null
            }
        }

        describe("DeliveryConfirmedEvent") {

            it("serializes and deserializes correctly") {
                val event =
                    DeliveryConfirmedEvent(
                        stopId = stopId,
                        packageId = packageId,
                        trackingNumber = TrackingNumber.generate("BLN"),
                        courierId = courierId,
                        customerId = customerId,
                        deliveredAt = now,
                        placement = DeliveryPlacement.FRONT_DOOR,
                        proofPhotoUrl = "https://s3.example.com/photos/test.jpg",
                        signatureUrl = null,
                        courierNote = "Left at front door",
                        finalStatus = DeliveryStatus.DELIVERED,
                    )

                val serialized = json.encodeToString(event)
                val deserialized = json.decodeFromString<DeliveryConfirmedEvent>(serialized)

                deserialized.placement shouldBe DeliveryPlacement.FRONT_DOOR
                deserialized.finalStatus shouldBe DeliveryStatus.DELIVERED
                deserialized.signatureUrl shouldBe null
                deserialized.deliveredAt shouldBe now
            }
        }

        describe("DeliveryFailedEvent") {

            it("preserves all fields through serialization") {
                val event =
                    DeliveryFailedEvent(
                        stopId = stopId,
                        packageId = packageId,
                        trackingNumber = TrackingNumber.generate("MUC"),
                        courierId = courierId,
                        customerId = customerId,
                        failedAt = now,
                        reason = FailureReason.NO_ANSWER,
                        courierNote = "Rang three times, no answer",
                        attemptNumber = 1,
                        willReschedule = true,
                    )

                val serialized = json.encodeToString(event)
                val deserialized = json.decodeFromString<DeliveryFailedEvent>(serialized)

                deserialized.reason shouldBe FailureReason.NO_ANSWER
                deserialized.attemptNumber shouldBe 1
                deserialized.willReschedule shouldBe true
            }
        }

        describe("StopAssignedEvent") {

            it("handles null optional fields correctly") {
                val event =
                    StopAssignedEvent(
                        stopId = stopId,
                        shiftId = shiftId,
                        courierId = courierId,
                        packageId = packageId,
                        customerId = customerId,
                        sequenceNumber = 3,
                        estimatedArrivalAt = null,
                    )

                val serialized = json.encodeToString(event)
                val deserialized = json.decodeFromString<StopAssignedEvent>(serialized)

                deserialized.estimatedArrivalAt shouldBe null
                deserialized.sequenceNumber shouldBe 3
            }
        }
    })
