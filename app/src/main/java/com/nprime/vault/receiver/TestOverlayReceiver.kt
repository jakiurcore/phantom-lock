package com.nprime.vault.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nprime.vault.overlay.LockOverlayService

class TestOverlayReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        ctx.startForegroundService(
            Intent(ctx, LockOverlayService::class.java)
                .setAction(LockOverlayService.ACTION_TEST_SHOW)
        )
    }
}
