package com.nprime.vault.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.nprime.vault.admin.DeviceOwnerManager
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.ui.lock.LockScreen
import com.nprime.vault.ui.lock.LockUiState
import com.nprime.vault.ui.theme.VaultTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LockOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val _vmStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = _vmStore
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val _uiState = MutableStateFlow(LockUiState())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var shouldLock = false
    private var relockJob: kotlinx.coroutines.Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    shouldLock = true
                    relockJob?.cancel()
                    relockJob = null
                }
                Intent.ACTION_SCREEN_ON -> if (shouldLock) showOverlay()
            }
        }
    }

    private val wipeCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == SilentWipeService.ACTION_WIPE_COMPLETE) hideOverlay()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        // Re-apply every start — ensures keyguard stays disabled after reboots/updates
        DeviceOwnerManager.applyPolicies(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }, Context.RECEIVER_NOT_EXPORTED)
        registerReceiver(wipeCompleteReceiver,
            IntentFilter(SilentWipeService.ACTION_WIPE_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_NOW) showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        instance = null
        hideOverlay()
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(wipeCompleteReceiver) } catch (_: Exception) {}
        scope.cancel()
        _vmStore.clear()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Overlay ───────────────────────────────────────────────────────────────
    private fun showOverlay() {
        relockJob?.cancel()
        relockJob = null
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) return

        _uiState.value = LockUiState()
        DeviceOwnerManager.setStatusBarLocked(this, true)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_SECURE or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.OPAQUE
        ).apply {
            @Suppress("DEPRECATION")
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@LockOverlayService)
            setViewTreeViewModelStoreOwner(this@LockOverlayService)
            setViewTreeSavedStateRegistryOwner(this@LockOverlayService)
            setContent {
                VaultTheme {
                    val state by _uiState.collectAsState()
                    LockScreen(state = state, onSubmit = ::evaluatePassword)
                }
            }
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) keyCode in CONSUMED_KEYS else false
            }
        }

        overlayView = view
        windowManager?.addView(view, params)
    }

    private fun hideOverlay() {
        val view = overlayView ?: return
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        windowManager?.removeView(view)
        overlayView = null
        shouldLock = false
        DeviceOwnerManager.setStatusBarLocked(this, false)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        scheduleRelock()
    }

    private fun scheduleRelock() {
        relockJob?.cancel()
        val delayMs = VaultPrefs.getAutoLockDelay(this)
        relockJob = scope.launch {
            delay(delayMs)
            showOverlay()
        }
    }

    // ── Password evaluation ───────────────────────────────────────────────────
    private fun evaluatePassword(password: String) {
        val ctx = applicationContext
        val lockoutUntil = VaultPrefs.getLockoutUntil(ctx)
        if (System.currentTimeMillis() < lockoutUntil) {
            triggerError(lockoutUntil); return
        }
        when {
            VaultPrefs.checkRealPin(ctx, password) -> {
                VaultPrefs.clearFailedAttempts(ctx)
                hideOverlay()
            }
            VaultPrefs.checkDuressPin(ctx, password) -> {
                VaultPrefs.clearFailedAttempts(ctx)
                _uiState.update { it.copy(isWiping = true) }
                startForegroundService(Intent(this, SilentWipeService::class.java))
            }
            else -> {
                val attempts = VaultPrefs.recordFailedAttempt(ctx)
                val maxAttempts = VaultPrefs.getMaxAttempts(ctx)
                if (attempts >= maxAttempts) {
                    // Exceeded limit — full factory reset, no warning
                    DeviceOwnerManager.wipeDevice(this)
                } else {
                    triggerError(VaultPrefs.getLockoutUntil(ctx))
                }
            }
        }
    }

    private fun triggerError(lockoutUntil: Long) {
        _uiState.update { it.copy(isError = true, lockoutUntil = lockoutUntil) }
        scope.launch { delay(800); _uiState.update { it.copy(isError = false) } }
    }

    // ── Notification ──────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "System Services", NotificationManager.IMPORTANCE_MIN).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("System")
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .build()

    companion object {
        var instance: LockOverlayService? = null
            private set

        val uiState: StateFlow<LockUiState>
            get() = instance?._uiState ?: MutableStateFlow(LockUiState())

        const val ACTION_SHOW_NOW = "com.nprime.vault.action.SHOW_NOW"
        private const val NOTIFICATION_ID = 100
        private const val CHANNEL_ID = "vault_system"

        private val CONSUMED_KEYS = setOf(
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_CAMERA, KeyEvent.KEYCODE_SEARCH,
            KeyEvent.KEYCODE_POWER
        )

        fun startAndShow(context: Context) {
            context.startForegroundService(
                Intent(context, LockOverlayService::class.java).setAction(ACTION_SHOW_NOW)
            )
        }

        fun start(context: Context) {
            context.startForegroundService(Intent(context, LockOverlayService::class.java))
        }
    }

    fun updateWipeMessage(message: String) {
        _uiState.update { it.copy(wipeMessage = message) }
    }
}
