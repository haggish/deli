plugins {
    id("deli.spring-service")
}

description = "API Gateway — Spring Cloud Gateway, JWT validation, request routing"

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // JWT validation
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
}

// Gateway does NOT produce a library jar — boot jar only
tasks.jar { enabled = false }
