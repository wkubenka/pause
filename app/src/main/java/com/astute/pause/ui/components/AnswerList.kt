package com.astute.pause.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.astute.pause.data.local.entity.Answer
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.domain.AnswerEvaluator

/** Warning signs use the soft tertiary container, which is noticeable without alarming. */
@Composable
fun AnswerList(
    answers: List<Answer>,
    onToggleFlag: ((Answer) -> Unit)?,
    onToggleFollowUpFlag: ((Answer) -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        answers.forEach { answer ->
            val highlighted = answer.flagged || answer.followUpFlagged
            val colors = if (highlighted) CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) else CardDefaults.cardColors()
            Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (highlighted) Text("Worth a second look", style = MaterialTheme.typography.labelSmall)
                    Row {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(answer.questionText, style = MaterialTheme.typography.bodyMedium)
                            Text(AnswerEvaluator.display(answer.answerType, answer.answer), style = MaterialTheme.typography.titleMedium)
                            if (answer.note.isNotBlank()) Text(answer.note, style = MaterialTheme.typography.bodySmall)
                        }
                        if (onToggleFlag != null && answer.answerType == AnswerType.TEXT) {
                            FlagButton(answer.flagged) { onToggleFlag(answer) }
                        }
                    }
                    if (answer.followUpQuestion.isNotBlank()) {
                        Row {
                            Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(answer.followUpQuestion, style = MaterialTheme.typography.bodyMedium)
                                Text(AnswerEvaluator.display(AnswerType.TEXT, answer.followUp), style = MaterialTheme.typography.titleMedium)
                            }
                            if (onToggleFollowUpFlag != null && answer.followUp.isNotBlank()) {
                                FlagButton(answer.followUpFlagged) { onToggleFollowUpFlag(answer) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagButton(flagged: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            if (flagged) Icons.Filled.Flag else Icons.Outlined.Flag,
            contentDescription = if (flagged) "Unflag" else "Flag as warning sign",
        )
    }
}
