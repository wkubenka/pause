package com.astute.pause.ui.followup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
fun FollowUpScreen(onBack: () -> Unit, onDone: () -> Unit, viewModel: FollowUpViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.item?.name.orEmpty()) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (state.alreadyAnswered) {
                Text("You've already looked back on this one. Thanks!")
                Button(onClick = onDone) { Text("Done") }
                return@Column
            }
            Text("It's been about 90 days.", style = MaterialTheme.typography.headlineSmall)
            YesNo("Do you remember buying it?", state.remembers, viewModel::setRemembers)
            YesNo("Are you happy with the purchase?", state.happy, viewModel::setHappy)
            Button(onClick = { viewModel.submit(onDone) }, enabled = state.canSubmit) { Text("Save") }
        }
    }
}

@Composable
private fun YesNo(question: String, value: Boolean?, onChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(question, style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = value == true, onClick = { onChange(true) }, label = { Text("Yes") })
            FilterChip(selected = value == false, onClick = { onChange(false) }, label = { Text("No") })
        }
    }
}
