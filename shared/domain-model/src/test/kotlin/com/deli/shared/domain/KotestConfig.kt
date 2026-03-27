package com.deli.shared.domain

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.IsolationMode
import kotlin.time.Duration.Companion.seconds

object KotestConfig : AbstractProjectConfig() {
    // Each test gets a fresh spec instance — no shared state between tests
    override val isolationMode = IsolationMode.InstancePerTest

    // Fail a test if it runs longer than 10 seconds — catches accidentally blocking calls
    override val timeout = 10.seconds

    // Show the full coroutine stack trace on failure
    override val coroutineDebugProbes = true
}
