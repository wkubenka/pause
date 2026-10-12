package com.astute.pause.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astute.pause.data.local.entity.TierMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TierEditScreen(onDone: () -> Unit, viewModel: TierEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New tier" else "Edit tier") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (!state.isNew) IconButton(onClick = { viewModel.delete(onDone) }) { Icon(Icons.Outlined.Delete, "Delete tier") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(state.name, { v -> viewModel.update { it.copy(name = v) } }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                state.minPrice, { v -> viewModel.update { it.copy(minPrice = v) } },
                label = { Text("Starts at (price)") },
                supportingText = { Text("Runs up to the next tier's starting price.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Questions", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.mode == TierMode.FULL, { viewModel.update { it.copy(mode = TierMode.FULL) } }, label = { Text("Full checklist") })
                FilterChip(state.mode == TierMode.SHORT, { viewModel.update { it.copy(mode = TierMode.SHORT) } }, label = { Text("Short list") })
            }
            Text("Wait", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    state.waitAmount, { v -> viewModel.update { it.copy(waitAmount = v) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                FilterChip(state.waitUnit == WaitUnit.HOURS, { viewModel.update { it.copy(waitUnit = WaitUnit.HOURS) } }, label = { Text("Hours") })
                FilterChip(state.waitUnit == WaitUnit.DAYS, { viewModel.update { it.copy(waitUnit = WaitUnit.DAYS) } }, label = { Text("Days") })
            }
            if (state.mode == TierMode.SHORT) {
                Text("Short list questions", style = MaterialTheme.typography.titleSmall)
                state.questions.forEach { q ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = q.id in state.selectedQuestionIds, onCheckedChange = { viewModel.toggleQuestion(q.id) })
                        Text(q.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { viewModel.save(onDone) }, modifier = Modifier.align(Alignment.End)) { Text("Save") }
        }
    }
}
