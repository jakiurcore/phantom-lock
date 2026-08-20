package com.nprime.vault.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager

object DeviceOwnerManager {

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun applyPolicies(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, DeviceOwnerReceiver::class.java)

        if (!dpm.isDeviceOwnerApp(context.packageName)) return

        // Disable the system keyguard — our overlay replaces it
        dpm.setKeyguardDisabled(admin, true)

        // Block bypass paths
        dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
        dpm.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER)
        dpm.addUserRestriction(admin, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
    }

    fun setStatusBarLocked(context: Context, locked: Boolean) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, DeviceOwnerReceiver::class.java)
        if (!dpm.isDeviceOwnerApp(context.packageName)) return
        dpm.setStatusBarDisabled(admin, locked)
    }

    fun setAdbBlocked(context: Context, blocked: Boolean) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, DeviceOwnerReceiver::class.java)
        if (!dpm.isDeviceOwnerApp(context.packageName)) return
        if (blocked) dpm.addUserRestriction(admin, UserManager.DISALLOW_DEBUGGING_FEATURES)
        else dpm.clearUserRestriction(admin, UserManager.DISALLOW_DEBUGGING_FEATURES)
    }

    fun isAdbBlocked(context: Context): Boolean {
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        return um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)
    }

    fun clearDeviceOwner(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, DeviceOwnerReceiver::class.java)

        if (!dpm.isDeviceOwnerApp(context.packageName)) return

        // Re-enable everything before clearing
        dpm.setKeyguardDisabled(admin, false)
        dpm.setStatusBarDisabled(admin, false)
        dpm.clearUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
        dpm.clearUserRestriction(admin, UserManager.DISALLOW_ADD_USER)
        dpm.clearUserRestriction(admin, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
        dpm.clearUserRestriction(admin, UserManager.DISALLOW_DEBUGGING_FEATURES)

        dpm.clearDeviceOwnerApp(context.packageName)
    }
}
