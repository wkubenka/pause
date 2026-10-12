package com.astute.pause.ui.walkthrough

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astute.pause.data.local.SeedData
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.Question

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkthroughScreen(
    recheck: Boolean,
    onExit: () -> Unit,
    onFinished: () -> Unit,
    viewModel: WalkthroughViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onFinished() }
    BackHandler { if (!viewModel.back()) onExit() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recheck) "Re-check: ${state.itemName}" else state.itemName) },
                navigationIcon = { IconButton(onClick = { if (!viewModel.back()) onExit() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.alreadyDecided) {
                Text("You've already decided on this one.", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onExit) { Text("Back home") }
                return@Column
            }
            val q = state.current ?: return@Column
            if (recheck && state.index == 0) {
                Text("Do you still want it? Are you calm about it?", style = MaterialTheme.typography.bodyLarge)
            }
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
            Text("${q.group.label} · ${state.index + 1} of ${state.questions.size}", style = MaterialTheme.typography.labelMedium)
            Text(q.text, style = MaterialTheme.typography.headlineSmall)
            AnswerInput(q, state.answer, viewModel::setAnswer)
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::next,
                enabled = state.canContinue,
                modifier = Modifier.align(Alignment.End),
            ) { Text(if (state.index == state.questions.lastIndex) "See summary" else "Next") }
        }
    }
}

@Composable
private fun AnswerInput(question: Question, answer: String, onAnswer: (String) -> Unit) {
    when (question.answerType) {
        AnswerType.YES_NO -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = answer == SeedData.YES, onClick = { onAnswer(SeedData.YES) }, label = { Text("Yes") })
            FilterChip(selected = answer == SeedData.NO, onClick = { onAnswer(SeedData.NO) }, label = { Text("No") })
        }
        AnswerType.CHOICE -> Column {
            question.options.forEach { option ->
                Row(
                    Modifier.fillMaxWidth()
                        .selectable(selected = answer == option, onClick = { onAnswer(option) }, role = Role.RadioButton)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = answer == option, onClick = null)
                    Text(option, Modifier.padding(start = 12.dp))
                }
            }
        }
        AnswerType.TEXT -> OutlinedTextField(
            value = answer,
            onValueChange = onAnswer,
            label = { Text("Your answer") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
