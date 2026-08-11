package com.nprime.vault.worker

import android.content.Context
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.overlay.LockOverlayService
import java.util.concurrent.TimeUnit

class WatchdogWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        if (VaultPrefs.isLockEnabled(applicationContext)) {
            LockOverlayService.start(applicationContext)
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "vault_watchdog"

        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES)
                .addTag(TAG)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                TAG,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelAllWorkByTag(TAG)
        }
    }
}
