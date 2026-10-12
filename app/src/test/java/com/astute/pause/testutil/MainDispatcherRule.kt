package com.astute.pause.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Installs a test Main dispatcher once per JVM and never swaps it back.
 *
 * ViewModels under test are never cleared, so their `stateIn(WhileSubscribed(5_000))` upstreams
 * keep running briefly after a test ends: when the last collector leaves, the sharing coroutine
 * calls `delay` on a Room worker thread, which reads `Dispatchers.Main`. Calling `resetMain()` /
 * `setMain()` at that moment fails with "Dispatchers.Main is used concurrently with setting it"
 * (seen intermittently on CI). Not writing Main again after the first install removes that race.
 */
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = installOnce()

    private companion object {
        @Volatile private var installed = false

        @Synchronized fun installOnce() {
            if (installed) return
            Dispatchers.setMain(UnconfinedTestDispatcher())
            installed = true
        }
    }
}
