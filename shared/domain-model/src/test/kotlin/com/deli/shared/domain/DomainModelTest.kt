package com.deli.shared.domain

import com.deli.shared.domain.model.DeliveryStatus
import com.deli.shared.domain.model.Package
import com.deli.shared.domain.model.PackageFlag
import com.deli.shared.domain.model.Shift
import com.deli.shared.domain.valueobject.Address
import com.deli.shared.domain.valueobject.Coordinates
import com.deli.shared.domain.valueobject.CourierId
import com.deli.shared.domain.valueobject.CustomerId
import com.deli.shared.domain.valueobject.Dimensions
import com.deli.shared.domain.valueobject.Money
import com.deli.shared.domain.valueobject.PackageId
import com.deli.shared.domain.valueobject.ShiftId
import com.deli.shared.domain.valueobject.TrackingNumber
import com.deli.shared.domain.valueobject.Weight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch

class ValueObjectTest :
    DescribeSpec({

        describe("TrackingNumber") {

            it("generates a valid tracking number for a city code") {
                val tn = TrackingNumber.generate("BLN")
                tn.value shouldMatch Regex("[A-Z]{2}-\\d{4}-\\d{4}-BLN")
            }

            it("rejects an invalid format") {
                shouldThrow<IllegalArgumentException> {
                    TrackingNumber("INVALID")
                }
            }

            it("rejects a city code that is not 3 letters") {
                shouldThrow<IllegalArgumentException> {
                    TrackingNumber.generate("BERLIN")
                }
            }
        }

        describe("Coordinates") {

            it("accepts valid latitude and longitude") {
                val coords = Coordinates(52.5200, 13.4050) // Berlin
                coords.latitude shouldBe 52.5200
                coords.longitude shouldBe 13.4050
            }

            it("rejects latitude above 90") {
                shouldThrow<IllegalArgumentException> { Coordinates(91.0, 0.0) }
            }

            it("rejects longitude below -180") {
                shouldThrow<IllegalArgumentException> { Coordinates(0.0, -181.0) }
            }

            it("calculates Haversine distance between two points correctly") {
                val berlin = Coordinates(52.5200, 13.4050)
                val munich = Coordinates(48.1351, 11.5820)
                val distanceKm = berlin.distanceTo(munich) / 1000
                // Berlin to Munich is approximately 504 km straight-line
                distanceKm shouldBeGreaterThan 490.0
                distanceKm shouldBeLessThan 520.0
            }

            it("returns zero distance for the same point") {
                val point = Coordinates(52.5200, 13.4050)
                point.distanceTo(point) shouldBeLessThan 0.001
            }
        }

        describe("Weight") {

            it("converts grams to kilograms correctly") {
                Weight.ofGrams(2500).toKg() shouldBe 2.5
            }

            it("creates weight from kilograms") {
                Weight.ofKg(1.5).grams shouldBe 1500
            }

            it("rejects zero weight") {
                shouldThrow<IllegalArgumentException> { Weight(0) }
            }

            it("rejects weight above 50kg") {
                shouldThrow<IllegalArgumentException> { Weight(50_001) }
            }
        }

        describe("Money") {

            it("creates zero money correctly") {
                val zero = Money.zero("EUR")
                zero.amountCents shouldBe 0L
                zero.currencyCode shouldBe "EUR"
            }

            it("creates money from euros") {
                val amount = Money.ofEuros(12.99)
                amount.amountCents shouldBe 1299L
                amount.toDecimal() shouldBe 12.99
            }

            it("rejects negative amounts") {
                shouldThrow<IllegalArgumentException> { Money(-1L, "EUR") }
            }

            it("rejects invalid currency codes") {
                shouldThrow<IllegalArgumentException> { Money(100L, "EURO") }
            }
        }

        describe("Address") {

            it("formats a full address correctly") {
                val address =
                    Address(
                        street = "Bergmannstraße",
                        houseNumber = "14",
                        apartment = "2B",
                        floor = 2,
                        city = "Berlin",
                        postalCode = "10961",
                        country = "DE",
                    )
                val formatted = address.formatted()
                formatted shouldBe "Bergmannstraße 14, Apt 2B, Floor 2, 10961 Berlin"
            }

            it("formats a simple address without apartment") {
                val address =
                    Address(
                        street = "Hauptstraße",
                        houseNumber = "1",
                        city = "München",
                        postalCode = "80333",
                        country = "DE",
                    )
                address.formatted() shouldBe "Hauptstraße 1, 80333 München"
            }
        }
    })

class PackageModelTest :
    DescribeSpec({

        val basePackage =
            Package(
                id = PackageId.new(),
                trackingNumber = TrackingNumber.generate("BLN"),
                customerId = CustomerId.new(),
                description = "Test package",
                weight = Weight.ofKg(2.0),
                dimensions = Dimensions(30, 20, 15),
                flags = setOf(PackageFlag.FRAGILE, PackageFlag.REQUIRES_SIGNATURE),
                status = DeliveryStatus.PENDING,
            )

        describe("Package flag helpers") {

            it("reports fragile correctly") {
                basePackage.isFragile() shouldBe true
            }

            it("reports signature requirement correctly") {
                basePackage.requiresSignature() shouldBe true
            }

            it("reports COD correctly when flag absent") {
                basePackage.isCashOnDelivery() shouldBe false
            }
        }
    })

class ShiftModelTest :
    DescribeSpec({

        describe("Shift progress calculation") {

            it("returns 0% progress for a new shift") {
                val shift =
                    Shift(
                        id = ShiftId.new(),
                        courierId = CourierId.new(),
                        scheduledDate = "2025-04-01",
                        totalStops = 10,
                        completedStops = 0,
                    )
                shift.progressPercent() shouldBe 0
                shift.remainingStops() shouldBe 10
            }

            it("calculates mid-shift progress correctly") {
                val shift =
                    Shift(
                        id = ShiftId.new(),
                        courierId = CourierId.new(),
                        scheduledDate = "2025-04-01",
                        totalStops = 10,
                        completedStops = 5,
                        failedStops = 1,
                    )
                shift.progressPercent() shouldBe 50
                shift.remainingStops() shouldBe 4
            }

            it("handles zero stops without division by zero") {
                val shift =
                    Shift(
                        id = ShiftId.new(),
                        courierId = CourierId.new(),
                        scheduledDate = "2025-04-01",
                        totalStops = 0,
                    )
                shift.progressPercent() shouldBe 0
            }
        }
    })
