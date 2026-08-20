package com.nprime.vault.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.IBinder
import com.nprime.vault.admin.DeviceOwnerManager
import com.nprime.vault.data.VaultPrefs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class SilentWipeService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // One deferred per package — supports parallel uninstalls
    private val pendingUninstalls = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    private val uninstallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pkg = intent.getStringExtra(EXTRA_PKG) ?: return
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            pendingUninstalls[pkg]?.complete(status == PackageInstaller.STATUS_SUCCESS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Starting…"))
        registerReceiver(
            uninstallReceiver,
            IntentFilter(ACTION_UNINSTALL_STATUS),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch { runWipe() }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        try { unregisterReceiver(uninstallReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Wipe orchestration ────────────────────────────────────────────────────

    private suspend fun runWipe() {
        val apps  = VaultPrefs.getSelectedApps(applicationContext)
        val files = VaultPrefs.getSelectedFiles(applicationContext)

        // ① Lock the device immediately: suspend every user app so nothing can open
        updateStatus(WipeStatus.LOCKING)
        DeviceOwnerManager.suspendAllUserApps(applicationContext)

        // ② Uninstall selected apps in parallel
        if (apps.isNotEmpty()) {
            updateStatus(WipeStatus.UNINSTALLING, apps.size)
            val jobs = apps.map { pkg ->
                scope.async {
                    try { uninstall(pkg) } catch (_: Exception) { false }
                }
            }
            jobs.awaitAll()
        }

        // ③ Delete selected files/folders in parallel
        if (files.isNotEmpty()) {
            updateStatus(WipeStatus.DELETING_FILES, files.size)
            val jobs = files.map { path ->
                scope.async {
                    try { File(path).deleteRecursively() } catch (_: Exception) {}
                }
            }
            jobs.awaitAll()
        }

        // ④ Release: unsuspend all remaining apps → phone is usable
        updateStatus(WipeStatus.RELEASING)
        DeviceOwnerManager.unsuspendAllUserApps(applicationContext)

        // ⑤ Signal done
        sendBroadcast(Intent(ACTION_WIPE_COMPLETE).setPackage(packageName))
        stopSelf()
    }

    // ── Silent uninstall ──────────────────────────────────────────────────────

    private suspend fun uninstall(packageName: String): Boolean {
        val installer = this.packageManager.packageInstaller
        val deferred = CompletableDeferred<Boolean>()
        pendingUninstalls[packageName] = deferred

        val callbackIntent = Intent(ACTION_UNINSTALL_STATUS).apply {
            setPackage(this@SilentWipeService.packageName)
            putExtra(EXTRA_PKG, packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            packageName.hashCode(),
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        return try {
            installer.uninstall(
                android.content.pm.VersionedPackage(packageName, PackageManager.VERSION_CODE_HIGHEST),
                pendingIntent.intentSender
            )
            // Wait up to 30s per app (uninstall confirmation should be auto-approved as DO)
            withTimeoutOrNull(30_000) { deferred.await() } ?: false
        } catch (_: Exception) {
            false
        } finally {
            pendingUninstalls.remove(packageName)
        }
    }

    // ── Progress messages to LockOverlay ─────────────────────────────────────

    private fun updateStatus(status: WipeStatus, count: Int = 0) {
        val msg = when (status) {
            WipeStatus.LOCKING         -> "Securing device…"
            WipeStatus.UNINSTALLING    -> "Removing $count app${if (count != 1) "s" else ""}…"
            WipeStatus.DELETING_FILES  -> "Erasing $count file path${if (count != 1) "s" else ""}…"
            WipeStatus.RELEASING       -> "Finalizing…"
        }
        // Update the overlay message
        LockOverlayService.instance?.updateWipeMessage(msg)
        // Update notification too
        updateNotification(msg)
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "System Services", NotificationManager.IMPORTANCE_MIN
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Preparing device…")
            .setContentText(text)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .build()

    companion object {
        const val ACTION_WIPE_COMPLETE   = "com.nprime.vault.action.WIPE_COMPLETE"
        const val ACTION_UNINSTALL_STATUS = "com.nprime.vault.action.UNINSTALL_STATUS"
        private const val EXTRA_PKG      = "pkg"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID     = "vault_system"
    }
}

private enum class WipeStatus { LOCKING, UNINSTALLING, DELETING_FILES, RELEASING }
