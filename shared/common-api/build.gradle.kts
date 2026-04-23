plugins {
    id("deli.kotlin-library")
}

description = "Shared OpenAPI DTOs and HTTP request/response models"

dependencies {
    api(project(":shared:domain-model"))
    api("jakarta.validation:jakarta.validation-api:3.1.1")
    implementation("org.springframework:spring-web:7.0.7")
    implementation("org.springframework:spring-context:7.0.7")
    implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")
    implementation("org.slf4j:slf4j-api:2.0.17")
}
