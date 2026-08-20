package com.nprime.vault.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DeviceOwnerReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Log.i(TAG, "Device Owner enabled — applying policies")
        DeviceOwnerManager.applyPolicies(context)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.i(TAG, "Device Owner disabled")
    }

    companion object {
        private const val TAG = "DeviceOwnerReceiver"
    }
}
