package com.nprime.vault.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nprime.vault.admin.DeviceOwnerManager
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.service.LockOverlayService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!VaultPrefs.isSetupComplete(context)) return
        DeviceOwnerManager.applyPolicies(context)
        LockOverlayService.startAndShow(context)
    }
}
