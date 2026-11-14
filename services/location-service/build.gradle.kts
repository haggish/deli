plugins {
    id("deli.spring-service")
}

description = "Location service — WebSocket GPS streaming, TimescaleDB, Redis position cache"

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.kafka)

    runtimeOnly(libs.postgresql.driver)

    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.postgres)

    implementation(project(":shared:common-api"))
    // Spring Kafka still uses Jackson 2.x internally; these explicit versions keep
    // the Kafka producer ObjectMapper on Jackson 2.x while web codecs use Jackson 3.x
    implementation(libs.jackson2.module.kotlin)
    implementation(libs.jackson2.datatype.jsr310)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.testcontainers.kafka)
}

tasks.jar { enabled = false }
