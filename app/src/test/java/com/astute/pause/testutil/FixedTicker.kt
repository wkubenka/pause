package com.astute.pause.testutil

import com.astute.pause.domain.NowTicker
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

class FixedTicker(instant: Instant) : NowTicker {
    override val now = MutableStateFlow(instant)
}
