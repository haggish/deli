plugins {
    id("deli.spring-service")
}

description = "Delivery service — confirmation, proof of delivery, S3 uploads"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.kafka:spring-kafka")

    runtimeOnly("org.postgresql:postgresql:42.7.4")

    implementation("org.flywaydb:flyway-core:10.22.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:10.22.0")

    // AWS SDK v2 for MinIO S3 pre-signed URLs
    implementation(platform("software.amazon.awssdk:bom:2.42.23"))
    implementation("software.amazon.awssdk:s3")

    implementation(project(":shared:common-api"))
    // Spring Kafka still uses Jackson 2.x internally; these explicit versions keep
    // the Kafka producer ObjectMapper on Jackson 2.x while web codecs use Jackson 3.x
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:kafka:1.20.4")
    testImplementation("org.testcontainers:localstack:1.20.4")
}

tasks.jar { enabled = false }
