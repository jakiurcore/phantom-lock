package com.nprime.vault.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nprime.vault.R
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.worker.WipeState
import com.nprime.vault.worker.WipeWorker

class LockOverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var prebuiltView: View
    private lateinit var overlayParams: WindowManager.LayoutParams
    private var overlayView: View? = null
    private val pinBuffer = StringBuilder()
    private val MAX_PIN = 8

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            Log.d(TAG, "screenReceiver: action=${intent.action}")
            when (intent.action) {
                Intent.ACTION_USER_PRESENT -> showOverlay()
                // ACTION_SCREEN_OFF intentionally does NOT remove the overlay.
                // On fingerprint/quick-unlock devices, SCREEN_OFF can arrive after
                // USER_PRESENT, which would flash the overlay and immediately dismiss it.
                // The overlay is only ever removed by a correct PIN or service destroy.
            }
        }
    }

    private val wipeCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            removeOverlay()
            stopSelf()
        }
    }

    companion object {
        private const val TAG = "VaultOverlay"
        private const val CHANNEL_ID = "vault_lock_monitor"
        private const val NOTIF_ID = 1
        const val ACTION_TEST_SHOW = "com.nprime.vault.TEST_SHOW_OVERLAY"

        @Volatile var isOverlayShowing = false

        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, LockOverlayService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, LockOverlayService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: service starting")
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        // Pre-build view and params so showOverlay() is a single wm.addView() call.
        val themedCtx = ContextThemeWrapper(this, R.style.Theme_Vault)
        prebuiltView = LayoutInflater.from(themedCtx).inflate(R.layout.overlay_lock, null)
        prebuiltView.isFocusableInTouchMode = true
        prebuiltView.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_BACK,
                    KeyEvent.KEYCODE_VOLUME_UP,
                    KeyEvent.KEYCODE_VOLUME_DOWN,
                    KeyEvent.KEYCODE_MENU,
                    KeyEvent.KEYCODE_CAMERA,
                    KeyEvent.KEYCODE_SEARCH -> return@setOnKeyListener true
                }
            }
            false
        }
        setupPinPad(prebuiltView)
        overlayParams = buildOverlayParams()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)

        val wipeFilter = IntentFilter(WipeState.ACTION_WIPE_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wipeCompleteReceiver, wipeFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wipeCompleteReceiver, wipeFilter)
        }
        Log.d(TAG, "onCreate: receiver registered, service ready")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        if (intent?.action == ACTION_TEST_SHOW) showOverlay()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: service being killed!")
        super.onDestroy()
        runCatching { unregisterReceiver(screenReceiver) }
        runCatching { unregisterReceiver(wipeCompleteReceiver) }
        removeOverlay()
    }

    // ── Overlay lifecycle ────────────────────────────────────────────────────

    private fun showOverlay() {
        if (overlayView != null) {
            // Screen turned off and back on without a correct PIN — reset UI for new attempt.
            Log.d(TAG, "showOverlay: already showing, resetting PIN state")
            pinBuffer.clear()
            prebuiltView.findViewById<TextView>(R.id.tv_pin_dots)?.text = ""
            prebuiltView.findViewById<TextView>(R.id.tv_pin_status)?.text = ""
            return
        }

        prebuiltView.translationY = 0f  // reset position cleared by previous dismissal animation
        try {
            wm.addView(prebuiltView, overlayParams)
            overlayView = prebuiltView
            isOverlayShowing = true
            Log.d(TAG, "showOverlay: wm.addView() SUCCESS")
        } catch (e: Exception) {
            Log.e(TAG, "showOverlay: wm.addView() FAILED: ${e.message}", e)
            return
        }
        prebuiltView.requestFocus()
        pinBuffer.clear()
    }

    private fun removeOverlay() {
        Log.d(TAG, "removeOverlay: called, had view=${overlayView != null}", Throwable("stack"))
        overlayView?.let {
            runCatching { wm.removeView(it) }
            overlayView = null
            isOverlayShowing = false
        }
        pinBuffer.clear()
    }

    /**
     * Swaps the pin pad content for a loading spinner inside the same window.
     * No window is added or removed so there is no z-order gap for the
     * system uninstaller to appear through.
     *
     * FLAG_NOT_FOCUSABLE is added so the uninstall dialog behind the overlay
     * becomes rootInActiveWindow for the accessibility service, letting it
     * find and click the confirm button in every visible window.
     */
    private fun showWipeLoadingInOverlay() {
        prebuiltView.findViewById<View>(R.id.wipe_loading_overlay)?.visibility = View.VISIBLE
        val wipeParams = buildOverlayParams().apply {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        runCatching { if (overlayView != null) wm.updateViewLayout(prebuiltView, wipeParams) }
    }

    private fun dismissOverlay(afterDismiss: (() -> Unit)? = null) {
        val view = overlayView ?: return
        view.animate()
            .translationY(-view.height.toFloat())
            .setDuration(280)
            .withEndAction {
                removeOverlay()
                afterDismiss?.invoke()
            }
            .start()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun buildOverlayParams(): WindowManager.LayoutParams {
        val statusBarH = statusBarHeight()
        val screenH    = screenHeight()
        val totalH     = screenH + statusBarH
        Log.d(TAG, "buildOverlayParams: statusBarH=$statusBarH screenH=$screenH totalH=$totalH")
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            totalH,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // FLAG_LAYOUT_IN_SCREEN  — lay out as if status bar doesn't exist
            // FLAG_LAYOUT_NO_LIMITS  — allow surface to extend above screen top
            // FLAG_SECURE            — block screenshots / recents thumbnail
            // (no FLAG_NOT_FOCUSABLE — window gets key events for back/volume interception)
            // (no FLAG_NOT_TOUCH_MODAL — ALL touches including status bar area go to us)
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            y = -statusBarH
        }
    }

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 72
    }

    private fun screenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.currentWindowMetrics.bounds.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            dm.heightPixels
        }
    }

    // ── PIN pad ──────────────────────────────────────────────────────────────

    private fun setupPinPad(view: View) {
        val tvDots     = view.findViewById<TextView>(R.id.tv_pin_dots)
        val tvStatus   = view.findViewById<TextView>(R.id.tv_pin_status)
        val btnDel     = view.findViewById<ImageButton>(R.id.btn_del)
        val btnConfirm = view.findViewById<ImageButton>(R.id.btn_confirm)

        val digitMap = mapOf(
            R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2",
            R.id.btn_3 to "3", R.id.btn_4 to "4", R.id.btn_5 to "5",
            R.id.btn_6 to "6", R.id.btn_7 to "7", R.id.btn_8 to "8",
            R.id.btn_9 to "9"
        )

        fun updateDots() { tvDots.text = "●".repeat(pinBuffer.length) }

        for ((id, digit) in digitMap) {
            view.findViewById<TextView>(id).setOnClickListener {
                if (pinBuffer.length < MAX_PIN) {
                    pinBuffer.append(digit)
                    updateDots()
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }
        }

        btnDel.setOnClickListener {
            if (pinBuffer.isNotEmpty()) {
                pinBuffer.deleteCharAt(pinBuffer.length - 1)
                updateDots()
            }
        }

        btnConfirm.setOnClickListener {
            if (pinBuffer.isEmpty()) return@setOnClickListener
            val input = pinBuffer.toString()
            pinBuffer.clear()
            updateDots()
            when {
                VaultPrefs.checkRealPin(this, input) -> dismissOverlay()
                VaultPrefs.checkDuressPin(this, input) -> {
                    showWipeLoadingInOverlay()
                    WorkManager.getInstance(this)
                        .enqueue(OneTimeWorkRequestBuilder<WipeWorker>().build())
                }
                else -> {
                    tvStatus.text = getString(R.string.wrong_pin)
                    tvDots.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake))
                }
            }
        }

        updateDots()
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Lock Screen", NotificationManager.IMPORTANCE_MIN)
        ch.setShowBadge(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
}
