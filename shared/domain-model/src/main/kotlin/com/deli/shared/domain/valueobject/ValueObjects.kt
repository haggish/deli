package com.deli.shared.domain.valueobject

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: UUID,
    ) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

@Serializable
@JvmInline
value class CourierId(
    @Serializable(with = UUIDSerializer::class) val value: UUID,
) {
    companion object {
        fun new(): CourierId = CourierId(UUID.randomUUID())

        fun of(value: String): CourierId = CourierId(UUID.fromString(value))

        fun of(value: UUID): CourierId = CourierId(value)
    }

    override fun toString(): String = value.toString()
}

@Serializable
@JvmInline
value class CustomerId(
    @Serializable(with = UUIDSerializer::class) val value: UUID,
) {
    companion object {
        fun new(): CustomerId = CustomerId(UUID.randomUUID())

        fun of(value: String): CustomerId = CustomerId(UUID.fromString(value))

        fun of(value: UUID): CustomerId = CustomerId(value)
    }

    override fun toString(): String = value.toString()
}

@Serializable
@JvmInline
value class PackageId(
    @Serializable(with = UUIDSerializer::class) val value: UUID,
) {
    companion object {
        fun new(): PackageId = PackageId(UUID.randomUUID())

        fun of(value: String): PackageId = PackageId(UUID.fromString(value))

        fun of(value: UUID): PackageId = PackageId(value)
    }

    override fun toString(): String = value.toString()
}

@Serializable
@JvmInline
value class StopId(
    @Serializable(with = UUIDSerializer::class) val value: UUID,
) {
    companion object {
        fun new(): StopId = StopId(UUID.randomUUID())

        fun of(value: String): StopId = StopId(UUID.fromString(value))

        fun of(value: UUID): StopId = StopId(value)
    }

    override fun toString(): String = value.toString()
}

@Serializable
@JvmInline
value class ShiftId(
    @Serializable(with = UUIDSerializer::class) val value: UUID,
) {
    companion object {
        fun new(): ShiftId = ShiftId(UUID.randomUUID())

        fun of(value: String): ShiftId = ShiftId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

@Serializable
@JvmInline
value class TrackingNumber(
    val value: String,
) {
    init {
        require(value.matches(Regex("[A-Z]{2}-\\d{4}-\\d{4}-[A-Z]{3}"))) {
            "Tracking number must match format XX-0000-0000-XXX, got: $value"
        }
    }

    companion object {
        private val CHARS = ('A'..'Z').toList()

        fun generate(cityCode: String): TrackingNumber {
            require(cityCode.length == 3) { "City code must be 3 letters" }
            val prefix = (1..2).map { CHARS.random() }.joinToString("")
            val mid1 = (1000..9999).random()
            val mid2 = (1000..9999).random()
            return TrackingNumber("$prefix-$mid1-$mid2-${cityCode.uppercase()}")
        }
    }

    override fun toString(): String = value
}

@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be in [-90, 90], got $latitude" }
        require(longitude in -180.0..180.0) { "Longitude must be in [-180, 180], got $longitude" }
    }

    fun distanceTo(other: Coordinates): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)
        val a =
            Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(latitude)) *
                Math.cos(Math.toRadians(other.latitude)) *
                Math.sin(dLon / 2).let { it * it }
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }
}

@Serializable
data class Address(
    val street: String,
    val houseNumber: String,
    val apartment: String? = null,
    val floor: Int? = null,
    val city: String,
    val postalCode: String,
    val country: String,
    val buzzerCode: String? = null,
    val deliveryInstructions: String? = null,
) {
    fun formatted(): String =
        buildString {
            append(street).append(" ").append(houseNumber)
            if (apartment != null) append(", Apt $apartment")
            if (floor != null) append(", Floor $floor")
            append(", ").append(postalCode).append(" ").append(city)
        }
}

@Serializable
data class Weight(
    val grams: Int,
) {
    init {
        require(grams > 0) { "Weight must be positive, got $grams" }
        require(grams <= 50_000) { "Weight exceeds 50kg maximum, got $grams" }
    }

    fun toKg(): Double = grams / 1000.0

    companion object {
        fun ofKg(kg: Double): Weight = Weight((kg * 1000).toInt())

        fun ofGrams(grams: Int): Weight = Weight(grams)
    }
}

@Serializable
data class Dimensions(
    val lengthCm: Int,
    val widthCm: Int,
    val heightCm: Int,
) {
    init {
        require(lengthCm > 0 && widthCm > 0 && heightCm > 0) {
            "All dimensions must be positive"
        }
    }

    fun volumeCm3(): Int = lengthCm * widthCm * heightCm
}

@Serializable
data class Money(
    val amountCents: Long,
    val currencyCode: String,
) {
    init {
        require(amountCents >= 0) { "Amount cannot be negative" }
        require(currencyCode.length == 3) { "Currency code must be ISO 4217 (3 chars)" }
    }

    fun toDecimal(): Double = amountCents / 100.0

    companion object {
        fun zero(currency: String) = Money(0L, currency)

        fun ofEuros(euros: Double) = Money((euros * 100).toLong(), "EUR")
    }
}
