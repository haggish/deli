plugins {
    id("deli.spring-service")
}

description = "Notification service — FCM push notifications via Kafka event consumers"

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.kafka)

    implementation(project(":shared:common-api"))

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.kafka)
}

tasks.jar { enabled = false }
