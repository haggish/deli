import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

// ── Root build file ───────────────────────────────────────────────────────────
// Subprojects inherit configuration via the convention plugins in
// buildSrc/src/main/kotlin. Nothing is configured here directly;
// this file exists to apply root-level plugins only.

// ── Shared configuration for ALL subprojects ──────────────────────────────────
subprojects {
    // Kotlin compiler options applied uniformly
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",           // null-safety for Spring annotations
                "-Xcontext-parameters",      // context parameters (replaces -Xcontext-receivers in Kotlin 2.3+)
            )
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    // Test configuration applied to every subproject
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.PASSED)
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = false
        }
        // Run tests in parallel within a module
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    }
}
