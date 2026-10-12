package com.astute.pause.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",
    val firstConsideredAt: Instant,
    val costCents: Long? = null,
    val currency: String,
    val tierId: Long? = null,
    val tierName: String? = null,
    val status: ItemStatus,
    val waitUntil: Instant? = null,
    val decidedAt: Instant? = null,
    val finalCostCents: Long? = null,
    val followupDueAt: Instant? = null,
    val remembersIt: Boolean? = null,
    val happyWithIt: Boolean? = null,
    val followupAnsweredAt: Instant? = null,
)

/** True when the item was marked Bought before its advisory wait ended. */
val Item.boughtEarly: Boolean
    get() {
        val decided = decidedAt ?: return false
        val until = waitUntil ?: return false
        return status == ItemStatus.BOUGHT && decided.isBefore(until)
    }
