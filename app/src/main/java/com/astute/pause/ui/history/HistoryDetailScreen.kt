package com.astute.pause.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astute.pause.domain.Money
import com.astute.pause.ui.components.AnswerList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(onBack: () -> Unit, viewModel: HistoryDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.item?.name.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val item = state.item ?: return@Column
            item.costCents?.let { Text("Price: ${Money.format(it, item.currency)} · ${item.tierName ?: "No tier"}") }
            item.finalCostCents?.let { Text("Paid: ${Money.format(it, item.currency)}") }
            if (item.followupAnsweredAt != null) {
                Text("Remembered buying it: ${if (item.remembersIt == true) "Yes" else "No"}")
                Text("Happy with it: ${if (item.happyWithIt == true) "Yes" else "No"}")
            }
            if (state.initial.isNotEmpty()) {
                Text("Checklist", style = MaterialTheme.typography.titleMedium)
                AnswerList(state.initial, onToggleFlag = null)
            }
            if (state.recheck.isNotEmpty()) {
                Text("Re-check", style = MaterialTheme.typography.titleMedium)
                AnswerList(state.recheck, onToggleFlag = null)
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this item?") },
            text = { Text("Its answers and follow-up will be removed too.") },
            confirmButton = { TextButton(onClick = { viewModel.delete(onBack) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
