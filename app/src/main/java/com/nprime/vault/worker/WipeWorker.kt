package com.nprime.vault.worker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nprime.vault.data.VaultPrefs
import kotlinx.coroutines.delay
import java.io.File

class WipeWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val pm  = ctx.packageManager

        val installedTargets = VaultPrefs.getSelectedApps(ctx)
            .filter { pkg -> pm.isInstalled(pkg) }

        WipeState.wipingPackages.clear()
        WipeState.wipingPackages.addAll(installedTargets)

        for (pkg in installedTargets) {
            try {
                val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
                delay(3_500L)
            } catch (_: Exception) { /* continue */ }
        }

        WipeState.wipingPackages.clear()

        // Remove packages that are now uninstalled from the stored list so a
        // subsequent wipe doesn't fire the system "App not found" dialog for them.
        val stillInstalled = VaultPrefs.getSelectedApps(ctx).filter { pm.isInstalled(it) }
        VaultPrefs.saveSelectedApps(ctx, stillInstalled)

        for (path in VaultPrefs.getSelectedFiles(ctx)) {
            try {
                val f = File(path)
                if (f.isDirectory) f.deleteRecursively() else f.delete()
            } catch (_: Exception) { /* continue */ }
        }

        if (VaultPrefs.shouldWipeSelf(ctx)) {
            VaultPrefs.clearAll(ctx)
        }

        ctx.sendBroadcast(Intent(WipeState.ACTION_WIPE_COMPLETE))

        return Result.success()
    }

    private fun PackageManager.isInstalled(pkg: String): Boolean = try {
        getPackageInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}

/** Shared in-process state so the accessibility service knows which dialogs to auto-click. */
object WipeState {
    const val ACTION_WIPE_COMPLETE = "com.nprime.vault.WIPE_COMPLETE"
    val wipingPackages = mutableSetOf<String>()
}
