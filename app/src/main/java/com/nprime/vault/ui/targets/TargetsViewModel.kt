package com.nprime.vault.ui.targets

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nprime.vault.data.VaultPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class AppEntry(val packageName: String, val label: String)

data class TreeNode(
    val path: String,
    val name: String,
    val isDir: Boolean,
    val depth: Int,
    val size: Long = 0,         // child count for dirs, bytes for files
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false
)

data class TargetsUiState(
    // Apps
    val apps: List<AppEntry> = emptyList(),
    val selectedApps: Set<String> = emptySet(),
    val isLoadingApps: Boolean = true,

    // Files
    val hasStoragePermission: Boolean = false,
    val treeNodes: List<TreeNode> = emptyList(),
    val selectedFiles: Set<String> = emptySet()
)

class TargetsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TargetsUiState())
    val uiState = _uiState.asStateFlow()

    fun load(context: Context) {
        _uiState.update {
            it.copy(
                selectedApps  = VaultPrefs.getSelectedApps(context),
                selectedFiles = VaultPrefs.getSelectedFiles(context),
                hasStoragePermission = Environment.isExternalStorageManager()
            )
        }
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { loadUserApps(context) }
            _uiState.update { it.copy(apps = apps, isLoadingApps = false) }
        }
        if (Environment.isExternalStorageManager()) initTree()
    }

    fun refreshPermission(context: Context) {
        val hasPerm = Environment.isExternalStorageManager()
        _uiState.update { it.copy(hasStoragePermission = hasPerm) }
        if (hasPerm && _uiState.value.treeNodes.isEmpty()) initTree()
    }

    // ── Tree ─────────────────────────────────────────────────────────────────

    private fun initTree() {
        val root = Environment.getExternalStorageDirectory()
        viewModelScope.launch {
            val rootNode = TreeNode(
                path = root.absolutePath,
                name = "Internal Storage",
                isDir = true,
                depth = 0,
                size = withContext(Dispatchers.IO) { root.listFiles()?.size?.toLong() ?: 0 }
            )
            // Pre-expand root immediately
            val rootChildren = withContext(Dispatchers.IO) { childrenOf(root, depth = 1) }
            val expanded = rootNode.copy(isExpanded = true)
            _uiState.update { it.copy(treeNodes = listOf(expanded) + rootChildren) }
        }
    }

    fun toggleExpand(path: String) {
        val nodes = _uiState.value.treeNodes.toMutableList()
        val idx = nodes.indexOfFirst { it.path == path }
        if (idx == -1) return
        val node = nodes[idx]

        if (node.isExpanded) {
            // Collapse: remove all descendants (depth > node.depth until next same-depth sibling)
            val toRemove = mutableListOf<Int>()
            for (i in idx + 1 until nodes.size) {
                if (nodes[i].depth > node.depth) toRemove.add(i) else break
            }
            toRemove.reversed().forEach { nodes.removeAt(it) }
            nodes[idx] = node.copy(isExpanded = false)
            _uiState.update { it.copy(treeNodes = nodes) }
        } else {
            // Expand: load children async
            nodes[idx] = node.copy(isLoading = true)
            _uiState.update { it.copy(treeNodes = nodes.toList()) }

            viewModelScope.launch {
                val children = withContext(Dispatchers.IO) {
                    childrenOf(File(path), depth = node.depth + 1)
                }
                val current = _uiState.value.treeNodes.toMutableList()
                val i = current.indexOfFirst { it.path == path }
                if (i == -1) return@launch
                current[i] = current[i].copy(isExpanded = true, isLoading = false)
                current.addAll(i + 1, children)
                _uiState.update { it.copy(treeNodes = current) }
            }
        }
    }

    private fun childrenOf(dir: File, depth: Int): List<TreeNode> {
        val files = try { dir.listFiles() ?: emptyArray() } catch (_: Exception) { emptyArray() }
        return files
            .filter { !it.name.startsWith(".") }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            .map { f ->
                TreeNode(
                    path = f.absolutePath,
                    name = f.name,
                    isDir = f.isDirectory,
                    depth = depth,
                    size = if (f.isDirectory) (f.listFiles()?.size ?: 0).toLong() else f.length()
                )
            }
    }

    // ── File selection ────────────────────────────────────────────────────────

    fun toggleFileSelection(context: Context, path: String) {
        val current = _uiState.value.selectedFiles.toMutableSet()
        if (path in current) current.remove(path) else current.add(path)
        _uiState.update { it.copy(selectedFiles = current) }
        VaultPrefs.saveSelectedFiles(context, current)
    }

    fun removeFileSelection(context: Context, path: String) {
        val current = _uiState.value.selectedFiles.toMutableSet()
        current.remove(path)
        _uiState.update { it.copy(selectedFiles = current) }
        VaultPrefs.saveSelectedFiles(context, current)
    }

    // ── Apps ──────────────────────────────────────────────────────────────────

    private fun loadUserApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .filter { it.packageName != context.packageName }
            .map { AppEntry(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    fun toggleApp(context: Context, pkg: String) {
        val current = _uiState.value.selectedApps.toMutableSet()
        if (pkg in current) current.remove(pkg) else current.add(pkg)
        _uiState.update { it.copy(selectedApps = current) }
        VaultPrefs.saveSelectedApps(context, current)
    }
}
