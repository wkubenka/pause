package com.astute.pause.ui.waiting

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astute.pause.ui.components.WaitingRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitingScreen(onBack: () -> Unit, onRecheck: (Long) -> Unit, viewModel: WaitingViewModel = hiltViewModel()) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Waiting") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        if (rows.isEmpty()) {
            Text("Nothing waiting right now.", Modifier.padding(padding).padding(16.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(rows, key = { it.item.id }) { row -> WaitingRow(row) { onRecheck(row.item.id) } }
            }
        }
    }
}
