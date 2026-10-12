package com.astute.pause.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tiers")
data class Tier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Inclusive lower bound. The upper bound is the next tier's minCents - 1. */
    val minCents: Long,
    val mode: TierMode,
    val waitHours: Int,
    /** Questions used when [mode] is SHORT. */
    val questionIds: List<Long> = emptyList(),
)
