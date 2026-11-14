plugins {
    id("deli.spring-service")
}

description = "API Gateway — Spring Cloud Gateway, JWT authentication, request routing"

dependencies {
    implementation(libs.spring.cloud.gateway.webflux)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.validation)

    // R2DBC reactive database access
    implementation(libs.spring.boot.starter.data.r2dbc)
    implementation(libs.r2dbc.postgresql)

    // Flyway — uses JDBC only for schema migrations, R2DBC for runtime queries
    implementation(libs.flyway.core)
    runtimeOnly(libs.flyway.postgres)
    runtimeOnly(libs.postgresql.driver)

    // JWT
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // Shared API models
    implementation(project(":shared:common-api"))

    // Testing
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.testcontainers.r2dbc)
    testImplementation(libs.reactor.test)
}

tasks.jar { enabled = false }
