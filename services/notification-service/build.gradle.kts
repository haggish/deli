plugins {
    id("deli.spring-service")
}

description = "Notification Service — Kafka consumer, FCM push, email/SMS dispatch"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.kafka:spring-kafka:3.3.1")

    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Firebase Admin SDK for FCM push notifications
    implementation("com.google.firebase:firebase-admin:9.4.2") {
        // Exclude Guava conflict with Spring
        exclude(group = "com.google.guava", module = "guava")
    }

    testImplementation("org.testcontainers:kafka:1.20.4")
    testImplementation("org.springframework.kafka:spring-kafka-test:3.3.1")
}
