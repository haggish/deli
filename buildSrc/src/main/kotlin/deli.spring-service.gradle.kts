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

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun lib(alias: String) = libs.findLibrary(alias).get()

group   = "com.deli"
version = "1.0.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

// Override Boot BOM's kotlinx-serialization (1.9.x) so services run the same version
// shared/domain-model is compiled against.
extra["kotlin-serialization.version"] = libs.findVersion("kotlinx-serialization").get().requiredVersion

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${libs.findVersion("spring-cloud").get().requiredVersion}")
    }
}

dependencies {
    // Shared library — every service depends on the domain model
    implementation(project(":shared:domain-model"))

    // Kotlin
    implementation(lib("kotlin-stdlib"))
    implementation(lib("kotlin-reflect"))
    implementation(lib("kotlinx-coroutines-core"))
    implementation(lib("kotlinx-coroutines-reactor"))

    // Observability — every service exposes /actuator/prometheus
    implementation(lib("spring-boot-starter-actuator"))
    runtimeOnly(lib("micrometer-prometheus"))

    // Testing
    testImplementation(lib("spring-boot-starter-test")) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation(lib("kotest-runner-junit5"))
    testImplementation(lib("kotest-assertions-core"))
    testImplementation(lib("mockk"))
    testImplementation(lib("testcontainers-junit5"))
    testImplementation(lib("testcontainers-core"))
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
    version.set(libs.findVersion("ktlint").get().requiredVersion)
    android.set(false)
}

/** detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
} */

configurations.matching { it.name.contains("detekt") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.findVersion("kotlin").get().requiredVersion)
        }
    }
}

// ktlint bundles its own Kotlin compiler; keep Spring dependency management
// from upgrading it to the project Kotlin version (ABI mismatch crashes ktlint).
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.2.21")
        }
    }
}
