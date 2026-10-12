package com.astute.pause.ui.summary

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astute.pause.data.local.entity.ItemStatus
import com.astute.pause.domain.Money
import com.astute.pause.ui.components.AnswerList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(onBack: () -> Unit, onDone: () -> Unit, viewModel: SummaryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingAction?.invoke(); pendingAction = null
    }
    fun withNotificationPermission(action: () -> Unit) {
        val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsAsk) { pendingAction = action; permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) } else action()
    }
    var showBoughtDialog by remember { mutableStateOf(false) }
    var finalCost by remember(state.item?.costCents) { mutableStateOf(state.item?.costCents?.let(Money::toInput) ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.item?.name.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val item = state.item ?: return@Column
            Text("Here's what you said", style = MaterialTheme.typography.headlineSmall)
            item.costCents?.let { Text("${Money.format(it, item.currency)} · ${item.tierName ?: "No tier"}") }
            Text(
                when (state.warningCount) {
                    0 -> "No warning signs."
                    1 -> "1 answer is worth a second look."
                    else -> "${state.warningCount} answers are worth a second look."
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            AnswerList(state.answers, onToggleFlag = if (state.isOpen) viewModel::toggleFlag else null)
            Text(
                "There's no right answer here, and the choice is yours. Not now doesn't have to mean never.",
                style = MaterialTheme.typography.bodyMedium,
            )

            if (!state.isOpen) {
                Text(if (item.status == ItemStatus.BOUGHT) "You bought this." else "You skipped this.")
                Button(onClick = onDone) { Text("Done") }
                return@Column
            }
            when {
                state.canStartWait -> Button(
                    onClick = { withNotificationPermission { viewModel.startWaiting(onDone) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Wait until ${state.decideOn}") }
                state.waitAlreadyOver && !state.recheck -> Text("Your wait is already over. Decide whenever you're ready.")
                item.status == ItemStatus.WAITING && !state.waitAlreadyOver ->
                    Text("Your wait ends ${state.decideOn}. You can still decide now.")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { viewModel.markSkipped(onDone) }, modifier = Modifier.weight(1f)) { Text("Skipped") }
                OutlinedButton(onClick = { showBoughtDialog = true }, modifier = Modifier.weight(1f)) { Text("Bought") }
            }
            if (item.status == ItemStatus.WAITING) {
                TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Keep waiting") }
            }
        }
    }

    if (showBoughtDialog) {
        AlertDialog(
            onDismissRequest = { showBoughtDialog = false },
            title = { Text("What did you pay?") },
            text = {
                OutlinedTextField(
                    value = finalCost,
                    onValueChange = { finalCost = it },
                    singleLine = true,
                    isError = state.error != null,
                    supportingText = state.error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    withNotificationPermission { viewModel.markBought(finalCost) { showBoughtDialog = false; onDone() } }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showBoughtDialog = false }) { Text("Cancel") } },
        )
    }
}
