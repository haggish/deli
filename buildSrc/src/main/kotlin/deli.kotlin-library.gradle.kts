plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

group   = "com.deli"
version = "1.0.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib:2.3.20")
    api("org.jetbrains.kotlin:kotlin-reflect:2.3.20")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.10.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.mockk:mockk:1.13.14")
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    version.set("1.8.0")
    android.set(false)
}
