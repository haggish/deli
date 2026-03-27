plugins {
    id("deli.kotlin-library")
}

description = "Shared OpenAPI DTOs and HTTP request/response models"

dependencies {
    api(project(":shared:domain-model"))
    api("jakarta.validation:jakarta.validation-api:3.1.0")
    // OpenAPI-generated models need Jackson annotations
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
}
