// buildSrc/src/main/kotlin/deli.spring-service.gradle.kts
//
// Apply to: all services/* subprojects
// Provides: Kotlin + Spring Boot fat-jar + Actuator + Prometheus + ktlint + detekt

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.jlleitschuh.gradle.ktlint")
    //id("io.gitlab.arturbosch.detekt")
}

group   = "com.deli"
version = "1.0.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
    }
}

dependencies {
    // Shared library — every service depends on the domain model
    implementation(project(":shared:domain-model"))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")

    // Observability — every service exposes /actuator/prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus:1.14.2")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.mockk:mockk:1.13.14")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:testcontainers:1.20.4")
}

// Docker image name follows convention: deli/<subproject-name>:local
tasks.bootBuildImage {
    imageName.set("deli/${project.name}:local")
    publish.set(false)
    environment.set(
        mapOf(
            "BP_JVM_VERSION" to "21",
            "BP_JVM_TYPE"    to "JRE",
        )
    )
}

ktlint {
    version.set("1.4.1")
    android.set(false)
}

/** detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
} */

configurations.matching { it.name.contains("detekt") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.1.0")
        }
    }
}
