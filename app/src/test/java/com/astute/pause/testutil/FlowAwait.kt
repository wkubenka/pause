package com.astute.pause.testutil

import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.seconds

suspend fun <T> Flow<T>.awaitFirst(predicate: (T) -> Boolean): T {
    var result: Result<T>? = null
    test(timeout = 5.seconds) {
        while (result == null) {
            val item = awaitItem()
            if (predicate(item)) result = Result.success(item)
        }
        cancelAndIgnoreRemainingEvents()
    }
    return result!!.getOrThrow()
}
