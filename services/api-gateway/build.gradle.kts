plugins {
    id("deli.spring-service")
}

description = "API Gateway — Spring Cloud Gateway, JWT authentication, request routing"

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // R2DBC reactive database access
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql:1.1.1.RELEASE")

    // Flyway — uses JDBC only for schema migrations, R2DBC for runtime queries
    implementation("org.flywaydb:flyway-core:10.22.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:10.22.0")
    runtimeOnly("org.postgresql:postgresql:42.7.11")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Shared API models
    implementation(project(":shared:common-api"))

    // Testing
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:r2dbc:1.21.4")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.jar { enabled = false }
