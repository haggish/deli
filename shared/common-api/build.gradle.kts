plugins {
    id("deli.kotlin-library")
}

description = "Shared OpenAPI DTOs and HTTP request/response models"

dependencies {
    api(project(":shared:domain-model"))
    api("jakarta.validation:jakarta.validation-api:3.1.0")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("org.springframework:spring-web:6.2.1")
    implementation("org.springframework:spring-context:6.2.1")
    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    implementation("org.slf4j:slf4j-api:2.0.13")
}
