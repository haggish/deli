plugins {
    id("deli.spring-service")
}

description = "Location service — WebSocket GPS streaming, TimescaleDB, Redis position cache"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.kafka:spring-kafka")

    runtimeOnly("org.postgresql:postgresql:42.7.4")

    implementation("org.flywaydb:flyway-core:10.22.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:10.22.0")

    implementation(project(":shared:common-api"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:kafka:1.20.4")
}

tasks.jar { enabled = false }
