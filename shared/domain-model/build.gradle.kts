plugins {
    id("deli.kotlin-library")
}

description = "Shared domain model: entities, value objects, Kafka event DTOs"

dependencies {
    // Explicitly declare serialization-core so the compiler plugin can see
    // KSerializer, Encoder, Decoder etc. during compilation of this module.
    // The convention plugin declares it as api() but the Kotlin compiler plugin
    // requires it on the direct compile classpath of each module that uses it.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.10.0")

    // Jakarta Validation annotations referenced in exception messages
    compileOnly("jakarta.validation:jakarta.validation-api:3.1.1")
}
