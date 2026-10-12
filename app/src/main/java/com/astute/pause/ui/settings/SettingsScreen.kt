package com.astute.pause.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
fun SettingsScreen(
    onBack: () -> Unit,
    onEditTier: (Long) -> Unit,
    onQuestions: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { Header("Tiers") }
            items(state.tiers, key = { it.tier.id }) { row ->
                ListItem(
                    modifier = Modifier.clickable { onEditTier(row.tier.id) },
                    headlineContent = { Text(row.tier.name) },
                    supportingContent = { Text("${row.range} · ${row.summary}") },
                )
            }
            item { ListItem(modifier = Modifier.clickable { onEditTier(0) }, headlineContent = { Text("Add a tier") }) }
            item { HorizontalDivider() }
            item {
                ListItem(
                    modifier = Modifier.clickable(onClick = onQuestions),
                    headlineContent = { Text("Questions") },
                    supportingContent = { Text("Edit, reorder or archive the checklist") },
                )
            }
            item { HorizontalDivider() }
            item { Header("Notifications") }
            item { Toggle("When a wait ends", state.notifications.remindersEnabled, viewModel::setReminders) }
            item { Toggle("One gentle nudge a day later", state.notifications.nudgeEnabled, viewModel::setNudge) }
            item { Toggle("90-day look back", state.notifications.followUpEnabled, viewModel::setFollowUp) }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}
