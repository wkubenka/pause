package com.astute.pause.ui.questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.QuestionGroup
import com.astute.pause.domain.AnswerEvaluator

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun QuestionEditScreen(onDone: () -> Unit, viewModel: QuestionEditViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New question" else "Edit question") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(state.text, { v -> viewModel.update { it.copy(text = v) } }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
            Text("Group", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuestionGroup.entries.forEach { g ->
                    FilterChip(state.group == g, { viewModel.update { it.copy(group = g) } }, label = { Text(g.label) })
                }
            }
            Text("Answer type", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.answerType == AnswerType.YES_NO, { viewModel.setType(AnswerType.YES_NO) }, label = { Text("Yes / no") })
                FilterChip(state.answerType == AnswerType.CHOICE, { viewModel.setType(AnswerType.CHOICE) }, label = { Text("Choice") })
                FilterChip(state.answerType == AnswerType.TEXT, { viewModel.setType(AnswerType.TEXT) }, label = { Text("Free text") })
            }
            if (state.answerType == AnswerType.CHOICE) {
                OutlinedTextField(
                    state.optionsText, { v -> viewModel.update { it.copy(optionsText = v) } },
                    label = { Text("Choices, one per line") }, minLines = 3, modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.warningChoices.isNotEmpty()) {
                Text("Which answers are warning signs?", style = MaterialTheme.typography.titleSmall)
                state.warningChoices.forEach { choice ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = choice in state.warningAnswers, onCheckedChange = { viewModel.toggleWarning(choice) })
                        Text(AnswerEvaluator.display(state.answerType, choice))
                    }
                }
            }
            if (state.answerType == AnswerType.CHOICE && state.options.isNotEmpty()) {
                Text("Follow-up questions (optional)", style = MaterialTheme.typography.titleSmall)
                Text("Asked as free text right after that choice is picked.", style = MaterialTheme.typography.bodySmall)
                state.options.forEach { option ->
                    OutlinedTextField(
                        state.followUps[option].orEmpty(), { v -> viewModel.setFollowUp(option, v) },
                        label = { Text("After \"$option\"") }, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (state.answerType == AnswerType.TEXT) {
                Text("You can flag free-text answers yourself on the summary.", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ask again in the re-check", Modifier.weight(1f))
                Switch(checked = state.inRecheck, onCheckedChange = { v -> viewModel.update { it.copy(inRecheck = v) } })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Active", Modifier.weight(1f))
                Switch(checked = state.active, onCheckedChange = { v -> viewModel.update { it.copy(active = v) } })
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { viewModel.save(onDone) }, modifier = Modifier.align(Alignment.End)) { Text("Save") }
        }
    }
}
