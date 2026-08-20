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
import java.util.concurrent.atomic.AtomicInteger

class SilentWipeService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        startForeground(NOTIFICATION_ID, buildNotification())
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

        // ① Start file deletion immediately in parallel — independent background job
        val fileDeletionJob = scope.launch(Dispatchers.IO) {
            files.map { path ->
                async { try { File(path).deleteRecursively() } catch (_: Exception) {} }
            }.awaitAll()
        }

        // ② Uninstall apps in parallel, reporting progress as each one finishes
        val total = apps.size
        val done = AtomicInteger(0)
        LockOverlayService.instance?.updateWipeProgress(0, total)

        if (apps.isNotEmpty()) {
            apps.map { pkg ->
                scope.async {
                    try { uninstall(pkg) } catch (_: Exception) { false }
                    val nowDone = done.incrementAndGet()
                    LockOverlayService.instance?.updateWipeProgress(nowDone, total)
                }
            }.awaitAll()
        }

        // ③ All apps done → loading screen dismisses, invisible blocker activates
        LockOverlayService.instance?.onAppsDone()

        // ④ Wait for file deletion to finish
        fileDeletionJob.join()

        // ⑤ Files also done → invisible blocker lifts, device fully usable
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
            withTimeoutOrNull(30_000) { deferred.await() } ?: false
        } catch (_: Exception) {
            false
        } finally {
            pendingUninstalls.remove(packageName)
        }
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

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Preparing device…")
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .build()

    companion object {
        const val ACTION_WIPE_COMPLETE    = "com.nprime.vault.action.WIPE_COMPLETE"
        const val ACTION_UNINSTALL_STATUS = "com.nprime.vault.action.UNINSTALL_STATUS"
        private const val EXTRA_PKG       = "pkg"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID      = "vault_system"
    }
}
