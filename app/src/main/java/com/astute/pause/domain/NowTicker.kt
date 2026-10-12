package com.astute.pause.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

interface NowTicker {
    val now: Flow<Instant>
}

class ClockTicker @Inject constructor(private val clock: Clock) : NowTicker {
    override val now: Flow<Instant> = flow {
        while (true) {
            emit(clock.instant())
            delay(60_000)
        }
    }
}
