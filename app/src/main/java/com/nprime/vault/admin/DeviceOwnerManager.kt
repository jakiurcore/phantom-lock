package com.nprime.vault.admin

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.util.Log

object DeviceOwnerManager {

    fun isSystemLockScreenSecure(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return km.isDeviceSecure
    }

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun applyPolicies(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, DeviceOwnerReceiver::class.java)

        if (!dpm.isDeviceOwnerApp(context.packageName)) return

        // Disable the system keyguard — our overlay replaces it
        // Requires device to have NO secure lock screen (None/Swipe); returns false if device has PIN/password/fingerprint
        val keyguardDisabled = dpm.setKeyguardDisabled(admin, true)
        Log.d("Vault/DO", "setKeyguardDisabled=$keyguardDisabled, isDeviceSecure=${isSystemLockScreenSecure(context)}")

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

    /** Silently factory-resets the device, wiping all user data and external storage. */
    fun wipeDevice(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!dpm.isDeviceOwnerApp(context.packageName)) return
        val flags = DevicePolicyManager.WIPE_EXTERNAL_STORAGE or
                    DevicePolicyManager.WIPE_RESET_PROTECTION_DATA or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        DevicePolicyManager.WIPE_SILENTLY else 0
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                dpm.wipeDevice(flags)
            } else {
                @Suppress("DEPRECATION")
                dpm.wipeData(flags)
            }
        } catch (e: Exception) {
            Log.e("Vault/DO", "wipeDevice failed: ${e.message}")
        }
    }

    /** Suspends all user apps so nothing can be launched. Returns the list suspended. */
    fun suspendAllUserApps(context: Context): Array<String> {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, DeviceOwnerReceiver::class.java)
        if (!dpm.isDeviceOwnerApp(context.packageName)) return emptyArray()

        val packages = context.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .filter { it.packageName != context.packageName }
            .map { it.packageName }
            .toTypedArray()

        return try {
            dpm.setPackagesSuspended(admin, packages, true) ?: emptyArray()
            packages
        } catch (e: Exception) {
            Log.e("Vault/DO", "suspendAllUserApps failed: ${e.message}")
            emptyArray()
        }
    }

    /** Unsuspends all user apps so the phone becomes usable again. */
    fun unsuspendAllUserApps(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, DeviceOwnerReceiver::class.java)
        if (!dpm.isDeviceOwnerApp(context.packageName)) return

        val packages = context.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .filter { it.packageName != context.packageName }
            .map { it.packageName }
            .toTypedArray()

        try {
            dpm.setPackagesSuspended(admin, packages, false)
        } catch (e: Exception) {
            Log.e("Vault/DO", "unsuspendAllUserApps failed: ${e.message}")
        }
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
