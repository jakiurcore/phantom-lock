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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

class SilentWipeService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentDeferred: CompletableDeferred<Boolean>? = null

    private val uninstallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            currentDeferred?.complete(status == PackageInstaller.STATUS_SUCCESS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerReceiver(uninstallReceiver, IntentFilter(ACTION_UNINSTALL_STATUS),
            Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            wipeTargets()
            broadcastComplete()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        try { unregisterReceiver(uninstallReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Wipe logic ────────────────────────────────────────────────────────────
    private suspend fun wipeTargets() {
        val apps  = VaultPrefs.getSelectedApps(applicationContext)
        val files = VaultPrefs.getSelectedFiles(applicationContext)

        apps.forEach { pkg ->
            try { silentUninstall(pkg) } catch (_: Exception) {}
        }

        files.forEach { path ->
            try { File(path).deleteRecursively() } catch (_: Exception) {}
        }
    }

    private suspend fun silentUninstall(packageName: String): Boolean {
        val installer = packageManager.packageInstaller
        val deferred = CompletableDeferred<Boolean>()
        currentDeferred = deferred

        val callbackIntent = Intent(ACTION_UNINSTALL_STATUS).setPackage(packageName)
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
            withTimeoutOrNull(12_000) { deferred.await() } ?: false
        } catch (_: Exception) {
            false
        } finally {
            currentDeferred = null
        }
    }

    private fun broadcastComplete() {
        val intent = Intent(ACTION_WIPE_COMPLETE).setPackage(packageName)
        sendBroadcast(intent)
    }

    // ── Notification ──────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "System Services", NotificationManager.IMPORTANCE_MIN
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Preparing device…")
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .build()

    companion object {
        const val ACTION_WIPE_COMPLETE     = "com.nprime.vault.action.WIPE_COMPLETE"
        const val ACTION_UNINSTALL_STATUS  = "com.nprime.vault.action.UNINSTALL_STATUS"
        private const val NOTIFICATION_ID  = 101
        private const val CHANNEL_ID       = "vault_system"
    }
}
