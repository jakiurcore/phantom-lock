package com.nprime.vault.ui.home

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import com.nprime.vault.admin.DeviceOwnerManager
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.service.LockOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val isDeviceOwner: Boolean = false,
    val isLockEnabled: Boolean = false,
    val isAdbBlocked: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val selectedApps: Int = 0,
    val selectedFiles: Int = 0,
    val showDeactivateDialog: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    fun refresh(context: Context) {
        _uiState.update {
            it.copy(
                isDeviceOwner = DeviceOwnerManager.isDeviceOwner(context),
                isLockEnabled = VaultPrefs.isLockEnabled(context),
                isAdbBlocked = DeviceOwnerManager.isAdbBlocked(context),
                hasOverlayPermission = Settings.canDrawOverlays(context),
                selectedApps = VaultPrefs.getSelectedApps(context).size,
                selectedFiles = VaultPrefs.getSelectedFiles(context).size
            )
        }
    }

    fun setLockEnabled(context: Context, enabled: Boolean) {
        VaultPrefs.setLockEnabled(context, enabled)
        _uiState.update { it.copy(isLockEnabled = enabled) }
        if (enabled) LockOverlayService.start(context)
        else LockOverlayService.instance?.stopSelf()
    }

    fun setAdbBlocked(context: Context, blocked: Boolean) {
        DeviceOwnerManager.setAdbBlocked(context, blocked)
        _uiState.update { it.copy(isAdbBlocked = blocked) }
    }

    fun showDeactivateDialog(show: Boolean) {
        _uiState.update { it.copy(showDeactivateDialog = show) }
    }

    fun deactivate(context: Context) {
        VaultPrefs.setLockEnabled(context, false)
        LockOverlayService.instance?.stopSelf()
        DeviceOwnerManager.clearDeviceOwner(context)
        _uiState.update { it.copy(isDeviceOwner = false, isLockEnabled = false, showDeactivateDialog = false) }
    }
}
