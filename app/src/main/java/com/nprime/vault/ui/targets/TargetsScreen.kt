package com.nprime.vault.ui.targets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TargetsScreen(vm: TargetsViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { vm.load(context) }

    val dirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) vm.addFile(context, uri.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Text(
            "Duress Targets",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.padding(24.dp, 24.dp, 24.dp, 0.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        ScrollableTabRow(
            selectedTabIndex = tab,
            containerColor = Color.Black,
            contentColor = Color.White,
            edgePadding = 0.dp
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("Apps", modifier = Modifier.padding(16.dp)) }
            Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("Files", modifier = Modifier.padding(16.dp)) }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                0 -> AppsTab(state, onToggle = { vm.toggleApp(context, it) })
                1 -> FilesTab(state, onRemove = { vm.removeFile(context, it) })
            }

            if (tab == 1) {
                FloatingActionButton(
                    onClick = { dirPicker.launch(null) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add folder", tint = Color.Black)
                }
            }
        }
    }
}

@Composable
private fun AppsTab(state: TargetsUiState, onToggle: (String) -> Unit) {
    if (state.isLoadingApps) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.apps, key = { it.packageName }) { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = app.packageName in state.selectedApps,
                    onCheckedChange = { onToggle(app.packageName) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.White,
                        checkmarkColor = Color.Black,
                        uncheckedColor = Color(0xFF666666)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(app.label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text(app.packageName, color = Color(0xFF666666), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun FilesTab(state: TargetsUiState, onRemove: (String) -> Unit) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
    ) {
        if (state.selectedFiles.isEmpty()) {
            item {
                Text(
                    "No paths selected. Tap + to add a folder.",
                    color = Color(0xFF666666),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
        items(state.selectedFiles.toList()) { path ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Uri.decode(path).removePrefix("content://com.android.externalstorage.documents/tree/"),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(path) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF888888))
                }
            }
        }
    }
}
