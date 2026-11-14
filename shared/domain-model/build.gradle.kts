plugins {
    id("deli.kotlin-library")
}

description = "Shared domain model: entities, value objects, Kafka event DTOs"

dependencies {
    // Explicitly declare serialization-core so the compiler plugin can see
    // KSerializer, Encoder, Decoder etc. during compilation of this module.
    // The convention plugin declares it as api() but the Kotlin compiler plugin
    // requires it on the direct compile classpath of each module that uses it.
    implementation(libs.kotlinx.serialization.core)

    // Jakarta Validation annotations referenced in exception messages
    compileOnly(libs.jakarta.validation.api)
}
