plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
    `java-library`
}

group   = "com.deli"
version = "1.0.0-SNAPSHOT"

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun lib(alias: String) = libs.findLibrary(alias).get()

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(lib("kotlin-stdlib"))
    api(lib("kotlin-reflect"))
    api(lib("kotlinx-serialization-core"))
    api(lib("kotlinx-serialization-json"))

    testImplementation(lib("kotest-runner-junit5"))
    testImplementation(lib("kotest-assertions-core"))
    testImplementation(lib("mockk"))
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    version.set(libs.findVersion("ktlint").get().requiredVersion)
    android.set(false)
}
