package com.astute.pause.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astute.pause.data.local.entity.Item
import com.astute.pause.data.local.entity.ItemStatus
import com.astute.pause.data.local.entity.boughtEarly
import com.astute.pause.domain.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Money not spent", style = MaterialTheme.typography.labelLarge)
                        Text(state.notSpent.ifEmpty { listOf("Nothing yet") }.joinToString(" · "), style = MaterialTheme.typography.headlineSmall)
                        val f = state.followUps
                        if (f.answered > 0) {
                            Text("Looked back on ${f.answered}: remembered ${f.remembered}, happy with ${f.happy}")
                        }
                    }
                }
            }
            items(state.items, key = { it.id }) { item -> HistoryRow(item) { onOpen(item.id) } }
        }
    }
}

@Composable
private fun HistoryRow(item: Item, onClick: () -> Unit) {
    val price = (item.finalCostCents ?: item.costCents)?.let { Money.format(it, item.currency) } ?: ""
    val outcome = when {
        item.status == ItemStatus.SKIPPED -> "Skipped"
        item.boughtEarly -> "Bought before the wait ended"
        else -> "Bought"
    }
    val followUp = when {
        item.followupAnsweredAt == null -> null
        item.happyWithIt == true -> "Happy with it"
        else -> "Not happy with it"
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(item.name) },
        supportingContent = { Text(listOfNotNull(outcome, followUp).joinToString(" · ")) },
        trailingContent = { Text(price) },
    )
}
