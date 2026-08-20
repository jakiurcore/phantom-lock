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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nprime.vault.ui.theme.Accent
import com.nprime.vault.ui.theme.Divider
import com.nprime.vault.ui.theme.Surface
import com.nprime.vault.ui.theme.TextSecondary

@Composable
fun TargetsScreen(vm: TargetsViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { vm.load(context) }

    val dirPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> if (uri != null) vm.addFile(context, uri.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header
        Text(
            text = "Duress Targets",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 4.dp)
        )
        Text(
            text = "Wiped silently when duress password is entered",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Tab row
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Black,
            contentColor = Color.White,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tab]),
                    color = Accent,
                    height = 2.dp
                )
            },
            divider = { HorizontalDivider(color = Divider, thickness = 0.5.dp) }
        ) {
            listOf("Apps", "Files").forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (tab == i) Color.White else TextSecondary
                        )
                    }
                )
            }
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
                        .padding(20.dp),
                    containerColor = Accent,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add folder")
                }
            }
        }
    }
}

@Composable
private fun AppsTab(state: TargetsUiState, onToggle: (String) -> Unit) {
    if (state.isLoadingApps) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            Text(
                "${state.selectedApps.size} selected",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
            )
        }
        item {
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
            ) {
                state.apps.forEachIndexed { idx, app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = app.packageName in state.selectedApps,
                            onCheckedChange = { onToggle(app.packageName) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Accent,
                                checkmarkColor = Color.White,
                                uncheckedColor = TextSecondary
                            )
                        )
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(app.label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                            Text(app.packageName, color = TextSecondary, style = MaterialTheme.typography.bodySmall,
                                maxLines = 1)
                        }
                    }
                    if (idx < state.apps.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = Divider,
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilesTab(state: TargetsUiState, onRemove: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp, )
            .padding(bottom = 88.dp)
    ) {
        if (state.selectedFiles.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No folders selected", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text("Tap + to add a folder to wipe", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            item {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface)
                ) {
                    val filesList = state.selectedFiles.toList()
                    filesList.forEachIndexed { idx, path ->
                        val display = Uri.decode(path)
                            .removePrefix("content://com.android.externalstorage.documents/tree/")
                            .replace("%3A", "/")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = display,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                                maxLines = 2
                            )
                            IconButton(onClick = { onRemove(path) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary)
                            }
                        }
                        if (idx < filesList.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                color = Divider,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
