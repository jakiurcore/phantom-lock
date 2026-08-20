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
    val isLockRunning: Boolean = false,
    val isAdbBlocked: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val systemLockSecure: Boolean = false,
    val maxAttempts: Int = VaultPrefs.DEFAULT_MAX_ATTEMPTS,
    val autoLockDelayMs: Long = VaultPrefs.DEFAULT_AUTO_LOCK_DELAY_MS,
    val selectedApps: Int = 0,
    val selectedFiles: Int = 0,
    val wipeOnFailEnabled: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    fun refresh(context: Context) {
        _uiState.update {
            it.copy(
                isDeviceOwner      = DeviceOwnerManager.isDeviceOwner(context),
                isLockRunning      = LockOverlayService.instance != null,
                isAdbBlocked       = DeviceOwnerManager.isAdbBlocked(context),
                hasOverlayPermission = Settings.canDrawOverlays(context),
                systemLockSecure   = DeviceOwnerManager.isSystemLockScreenSecure(context),
                maxAttempts        = VaultPrefs.getMaxAttempts(context),
                autoLockDelayMs    = VaultPrefs.getAutoLockDelay(context),
                selectedApps       = VaultPrefs.getSelectedApps(context).size,
                selectedFiles      = VaultPrefs.getSelectedFiles(context).size,
                wipeOnFailEnabled  = VaultPrefs.isWipeOnFailEnabled(context)
            )
        }
    }

    fun setAdbBlocked(context: Context, blocked: Boolean) {
        DeviceOwnerManager.setAdbBlocked(context, blocked)
        _uiState.update { it.copy(isAdbBlocked = blocked) }
    }

    fun setMaxAttempts(context: Context, n: Int) {
        VaultPrefs.saveMaxAttempts(context, n)
        _uiState.update { it.copy(maxAttempts = n.coerceIn(VaultPrefs.MIN_ATTEMPTS, VaultPrefs.MAX_ATTEMPTS_LIMIT)) }
    }

    fun setAutoLockDelay(context: Context, ms: Long) {
        DeviceOwnerManager.setMaximumTimeLock(context, ms)
        _uiState.update { it.copy(autoLockDelayMs = ms) }
    }

    fun setWipeOnFail(context: Context, enabled: Boolean) {
        VaultPrefs.setWipeOnFail(context, enabled)
        _uiState.update { it.copy(wipeOnFailEnabled = enabled) }
    }
}
