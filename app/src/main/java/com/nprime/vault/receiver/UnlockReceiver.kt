package com.nprime.vault.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.overlay.LockOverlayService

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED") {
            if (VaultPrefs.isLockEnabled(ctx)) {
                LockOverlayService.start(ctx)
            }
        }
    }
}
