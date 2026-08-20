package com.nprime.vault.ui.targets

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nprime.vault.data.VaultPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppEntry(val packageName: String, val label: String)

data class TargetsUiState(
    val apps: List<AppEntry> = emptyList(),
    val selectedApps: MutableSet<String> = mutableSetOf(),
    val selectedFiles: MutableSet<String> = mutableSetOf(),
    val isLoadingApps: Boolean = true
)

class TargetsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TargetsUiState())
    val uiState = _uiState.asStateFlow()

    fun load(context: Context) {
        val savedApps = VaultPrefs.getSelectedApps(context).toMutableSet()
        val savedFiles = VaultPrefs.getSelectedFiles(context).toMutableSet()
        _uiState.update { it.copy(selectedApps = savedApps, selectedFiles = savedFiles) }

        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { loadUserApps(context) }
            _uiState.update { it.copy(apps = apps, isLoadingApps = false) }
        }
    }

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

    fun addFile(context: Context, path: String) {
        val current = _uiState.value.selectedFiles.toMutableSet()
        current.add(path)
        _uiState.update { it.copy(selectedFiles = current) }
        VaultPrefs.saveSelectedFiles(context, current)
    }

    fun removeFile(context: Context, path: String) {
        val current = _uiState.value.selectedFiles.toMutableSet()
        current.remove(path)
        _uiState.update { it.copy(selectedFiles = current) }
        VaultPrefs.saveSelectedFiles(context, current)
    }
}
