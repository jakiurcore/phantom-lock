package com.nprime.vault.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.nprime.vault.overlay.LockOverlayService
import com.nprime.vault.worker.WipeState

class DuressAccessibilityService : AccessibilityService() {

    companion object { private const val TAG = "VaultA11y" }

    private var lastDismissMs = 0L

    private val confirmLabels = listOf(
        "ok", "okay", "uninstall", "delete", "yes", "confirm", "remove",
        "حذف", "удалить", "删除", "désinstaller", "deinstallieren",
        "desinstalar", "삭제", "削除"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // ── Auto-click uninstall dialogs during wipe ─────────────────────────
        if (WipeState.wipingPackages.isNotEmpty()) {
            val type = event.eventType
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {

                // 1. event.source is freshest — the node that actually changed.
                //    With a non-focusable overlay the uninstall dialog fires its
                //    own WINDOW_STATE_CHANGED so source points directly at it.
                val src = event.source ?: rootInActiveWindow
                if (src != null && clickConfirmButton(src)) return

                // 2. Walk every non-own-app window as a safety net. Skipping our
                //    own package prevents the PIN-pad confirm button (still present
                //    in the view tree under the loading overlay) from being matched.
                for (window in windows) {
                    if (window.root?.packageName?.toString() == packageName) continue
                    val root = window.root ?: continue
                    if (clickConfirmButton(root)) return
                }
            }
            return
        }

        // ── Dismiss notification shade while overlay is on screen ────────────
        if (LockOverlayService.isOverlayShowing) {
            val pkg = event.packageName?.toString() ?: return
            val isSystemUI = pkg == "com.android.systemui" || pkg == "com.oplus.systemui" ||
                    pkg == "com.miui.systemui" || pkg == "com.samsung.android.systemui"
            if (isSystemUI) {
                val now = System.currentTimeMillis()
                if (now - lastDismissMs > 500) {
                    lastDismissMs = now
                    Log.d(TAG, "systemUI event: type=${event.eventType} → dismissShade")
                    dismissShade()
                }
            }
        }
    }

    private fun dismissShade() {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
        Log.d(TAG, "dismissShade: result=$result api=${Build.VERSION.SDK_INT}")
    }

    private fun clickConfirmButton(node: AccessibilityNodeInfo): Boolean {
        for (label in confirmLabels) {
            val results = node.findAccessibilityNodeInfosByText(label)
            for (result in results) {
                if (result.isClickable) {
                    result.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                var parent = result.parent
                var depth = 0
                while (parent != null && depth < 4) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                    parent = parent.parent
                    depth++
                }
            }
        }
        return false
    }

    override fun onInterrupt() {}
}
