package com.astute.pause.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astute.pause.data.local.entity.Item
import com.astute.pause.ui.components.WaitingRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onItemCreated: (Long) -> Unit,
    onResume: (Item) -> Unit,
    onRecheck: (Long) -> Unit,
    onFollowUp: (Long) -> Unit,
    onSeeAllWaiting: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val submit = { viewModel.start(name) { id -> name = ""; onItemCreated(id) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pause") },
                actions = {
                    IconButton(onClick = onHistory) { Icon(Icons.Outlined.History, contentDescription = "History") }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("What are you thinking of buying?") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { submit() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                    Button(onClick = submit, enabled = name.isNotBlank(), modifier = Modifier.align(Alignment.End)) { Text("Next") }
                }
            }
            if (state.followUpsDue.isNotEmpty()) {
                item { SectionHeader("Looking back") }
                items(state.followUpsDue, key = { "f${it.id}" }) { item ->
                    ListItem(
                        modifier = Modifier.clickable { onFollowUp(item.id) },
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text("Two quick questions, 90 days on") },
                    )
                }
            }
            if (state.waiting.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader("Waiting", Modifier.weight(1f))
                        TextButton(onClick = onSeeAllWaiting) { Text("See all") }
                    }
                }
                items(state.waiting, key = { "w${it.item.id}" }) { row -> WaitingRow(row) { onRecheck(row.item.id) } }
            }
            if (state.inProgress.isNotEmpty()) {
                item { SectionHeader("Still deciding") }
                items(state.inProgress, key = { "p${it.id}" }) { item ->
                    ListItem(
                        modifier = Modifier.clickable { onResume(item) },
                        headlineContent = { Text(item.name) },
                        supportingContent = { Text(if (item.costCents == null) "Add a price" else "Pick up where you left off") },
                        trailingContent = {
                            IconButton(onClick = { viewModel.discard(item.id) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Discard ${item.name}")
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
