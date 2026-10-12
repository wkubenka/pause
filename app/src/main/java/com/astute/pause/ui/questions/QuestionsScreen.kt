package com.astute.pause.ui.questions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(onBack: () -> Unit, onEdit: (Long) -> Unit, viewModel: QuestionsViewModel = hiltViewModel()) {
    val questions by viewModel.questions.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Questions") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { onEdit(0) }) { Icon(Icons.Filled.Add, "Add question") } },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            itemsIndexed(questions, key = { _, q -> q.id }) { index, q ->
                ListItem(
                    modifier = Modifier.clickable { onEdit(q.id) },
                    headlineContent = { Text(q.text) },
                    supportingContent = {
                        Text(listOfNotNull(q.group.label, if (q.inRecheck) "Re-check" else null, if (!q.active) "Archived" else null).joinToString(" · "))
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { viewModel.move(q.id, up = true) }, enabled = index > 0) { Icon(Icons.Filled.KeyboardArrowUp, "Move up") }
                            IconButton(onClick = { viewModel.move(q.id, up = false) }, enabled = index < questions.lastIndex) { Icon(Icons.Filled.KeyboardArrowDown, "Move down") }
                            Switch(checked = q.active, onCheckedChange = { viewModel.setActive(q.id, it) })
                        }
                    },
                )
            }
            item {
                Text(
                    "Default questions are from The Smart Spending Checklist by Sunny Kind Journey, for personal use.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
