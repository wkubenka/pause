package com.astute.pause.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import com.astute.pause.data.local.entity.Item
import com.astute.pause.domain.Countdown
import java.time.Instant
import java.time.ZoneId

data class WaitingRowModel(val item: Item, val countdown: String, val decideOn: String, val ready: Boolean)

fun waitingRowModel(item: Item, now: Instant, zone: ZoneId): WaitingRowModel {
    val until = item.waitUntil ?: now
    return WaitingRowModel(
        item = item,
        countdown = Countdown.describe(now, until),
        decideOn = Countdown.decideOn(until, zone),
        ready = !until.isAfter(now),
    )
}

@Composable
fun WaitingRow(model: WaitingRowModel, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(model.item.name) },
        supportingContent = { Text("Decide on ${model.decideOn}") },
        trailingContent = {
            Column {
                Text(
                    model.countdown,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (model.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
