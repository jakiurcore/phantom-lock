package com.nprime.vault.ui.targets

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nprime.vault.ui.theme.Accent
import com.nprime.vault.ui.theme.Danger
import com.nprime.vault.ui.theme.Divider
import com.nprime.vault.ui.theme.Surface
import com.nprime.vault.ui.theme.SurfaceHigh
import com.nprime.vault.ui.theme.TextSecondary
import com.nprime.vault.ui.theme.Warning

@Composable
fun TargetsScreen(vm: TargetsViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { vm.load(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Duress Targets",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
                Text(
                    "Wiped silently when duress password is entered",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            val count = if (tab == 0) state.selectedApps.size else state.selectedFiles.size
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Danger)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$count",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }

        // ── Tabs ─────────────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Black,
            contentColor = Color.White,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tab]),
                    color = Accent, height = 2.dp
                )
            },
            divider = { HorizontalDivider(color = Divider, thickness = 0.5.dp) }
        ) {
            listOf("Apps", "Files").forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onClick = {
                        tab = i
                        if (i == 1) vm.refreshPermission(context)
                    },
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

        // ── Content ───────────────────────────────────────────────────────────
        when (tab) {
            0 -> AppsTab(state, onToggle = { vm.toggleApp(context, it) })
            1 -> FilesTab(
                state = state,
                onToggleExpand = { vm.toggleExpand(it) },
                onToggleSelect = { vm.toggleFileSelection(context, it) },
                onRemoveSelect = { vm.removeFileSelection(context, it) },
                onGrantPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            )
        }
    }
}

// ── Apps tab ──────────────────────────────────────────────────────────────────

@Composable
private fun AppsTab(state: TargetsUiState, onToggle: (String) -> Unit) {
    if (state.isLoadingApps) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            Text(
                "${state.selectedApps.size} selected — silently uninstalled on duress",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
            )
        }
        item {
            Column(Modifier.clip(RoundedCornerShape(12.dp)).background(Surface)) {
                state.apps.forEachIndexed { idx, app ->
                    val selected = app.packageName in state.selectedApps
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(app.packageName) }
                            .background(if (selected) Danger.copy(alpha = 0.07f) else Color.Transparent)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { onToggle(app.packageName) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Danger,
                                checkmarkColor = Color.White,
                                uncheckedColor = TextSecondary
                            )
                        )
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(app.label, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                app.packageName, color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (idx < state.apps.lastIndex)
                        HorizontalDivider(Modifier.padding(start = 52.dp), color = Divider, thickness = 0.5.dp)
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

// ── Files tab ─────────────────────────────────────────────────────────────────

@Composable
private fun FilesTab(
    state: TargetsUiState,
    onToggleExpand: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    onRemoveSelect: (String) -> Unit,
    onGrantPermission: () -> Unit
) {
    if (!state.hasStoragePermission) {
        PermissionPrompt(onGrant = onGrantPermission)
        return
    }

    if (state.treeNodes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // Selected summary header
        if (state.selectedFiles.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E1A0E))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        "${state.selectedFiles.size} path(s) marked for wipe",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(Modifier.height(4.dp))
                    state.selectedFiles.forEach { path ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📁  ${shortPath(path)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "✕",
                                color = Danger,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .clickable { onRemoveSelect(path) }
                                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }
        }

        // Tree nodes
        items(items = state.treeNodes, key = { it.path }) { node ->
            TreeRow(
                node = node,
                isSelected = node.path in state.selectedFiles,
                onToggleExpand = { onToggleExpand(node.path) },
                onToggleSelect = { onToggleSelect(node.path) }
            )
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun TreeRow(
    node: TreeNode,
    isSelected: Boolean,
    onToggleExpand: () -> Unit,
    onToggleSelect: () -> Unit
) {
    val indentPerLevel = 16.dp
    val indent = indentPerLevel * node.depth

    val chevronAngle by animateFloatAsState(
        targetValue = if (node.isExpanded) 180f else 0f,
        label = "chevron"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Danger.copy(alpha = 0.09f) else Color.Transparent)
            .clickable { if (node.isDir) onToggleExpand() else onToggleSelect() }
            .padding(start = indent + 8.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expand arrow for dirs / file emoji icon
        if (node.isDir) {
            if (node.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = TextSecondary,
                    strokeWidth = 1.5.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (node.isExpanded) "Collapse" else "Expand",
                    tint = if (node.isExpanded) Color.White else TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(chevronAngle)
                )
            }
        } else {
            Text(
                fileEmoji(node.name),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Folder / file icon box
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(
                    when {
                        node.isDir && node.depth == 0 -> Color(0xFF1C2C4C)
                        node.isDir -> Color(0xFF1A1A2E)
                        else -> SurfaceHigh
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (node.isDir) "📁" else fileEmoji(node.name),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) Color.White else if (node.isDir) Color.White else Color(0xFFCCCCCC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (node.isDir) "${node.size} items" else formatSize(node.size),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Selection checkbox
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelect() },
            colors = CheckboxDefaults.colors(
                checkedColor = Danger,
                checkmarkColor = Color.White,
                uncheckedColor = TextSecondary.copy(alpha = 0.4f)
            )
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = indent + 60.dp),
        color = Divider,
        thickness = 0.5.dp
    )
}

// ── Permission prompt ─────────────────────────────────────────────────────────

@Composable
private fun PermissionPrompt(onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(Warning.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Warning, null, tint = Warning, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Storage Access Required", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            "Vault needs \"All Files Access\" to browse your storage and delete files on duress. " +
            "Without it, only apps can be targeted.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGrant,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Grant All Files Access") }
        Spacer(Modifier.height(8.dp))
        Text(
            "Settings → Apps → Vault → Permissions → Files → Allow management of all files",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun shortPath(path: String): String {
    val root = "/storage/emulated/0"
    return when {
        path == root -> "Internal Storage"
        path.startsWith("$root/") -> path.removePrefix("$root/")
        else -> path
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L         -> "%.0f KB".format(bytes / 1_024.0)
    else                    -> "$bytes B"
}

private fun fileEmoji(name: String): String = when {
    name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) ||
    name.endsWith(".png", true) || name.endsWith(".webp", true) ||
    name.endsWith(".heic", true) || name.endsWith(".gif", true) -> "🖼️"
    name.endsWith(".mp4", true) || name.endsWith(".mkv", true) ||
    name.endsWith(".avi", true) || name.endsWith(".mov", true) -> "🎬"
    name.endsWith(".mp3", true) || name.endsWith(".m4a", true) ||
    name.endsWith(".flac", true) || name.endsWith(".ogg", true) -> "🎵"
    name.endsWith(".pdf", true) -> "📄"
    name.endsWith(".zip", true) || name.endsWith(".rar", true) ||
    name.endsWith(".gz", true)  || name.endsWith(".tar", true) -> "🗜️"
    name.endsWith(".apk", true) -> "📦"
    name.endsWith(".txt", true) || name.endsWith(".log", true) -> "📝"
    else -> "📄"
}
