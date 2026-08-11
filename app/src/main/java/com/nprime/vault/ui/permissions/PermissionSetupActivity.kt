package com.nprime.vault.ui.permissions

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nprime.vault.R
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.service.DuressAccessibilityService
import com.nprime.vault.ui.home.HomeActivity
import com.nprime.vault.worker.WatchdogWorker

class PermissionSetupActivity : AppCompatActivity() {

    private lateinit var cardOverlay: MaterialCardView
    private lateinit var chipOverlay: Chip
    private lateinit var cardAccessibility: MaterialCardView
    private lateinit var chipAccessibility: Chip
    private lateinit var cardFiles: MaterialCardView
    private lateinit var chipFiles: Chip
    private lateinit var cardBattery: MaterialCardView
    private lateinit var chipBattery: Chip
    private lateinit var btnContinue: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_setup)

        cardOverlay       = findViewById(R.id.card_overlay)
        chipOverlay       = findViewById(R.id.chip_overlay)
        cardAccessibility = findViewById(R.id.card_accessibility)
        chipAccessibility = findViewById(R.id.chip_accessibility)
        cardFiles         = findViewById(R.id.card_files)
        chipFiles         = findViewById(R.id.chip_files)
        cardBattery       = findViewById(R.id.card_battery)
        chipBattery       = findViewById(R.id.chip_battery)
        btnContinue       = findViewById(R.id.btn_continue)

        cardOverlay.setOnClickListener { requestOverlay() }
        cardAccessibility.setOnClickListener { requestAccessibility() }
        cardFiles.setOnClickListener { requestFileAccess() }
        cardBattery.setOnClickListener { requestBatteryExemption() }
        btnContinue.setOnClickListener { proceed() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val overlayOk  = Settings.canDrawOverlays(this)
        val accessOk   = isAccessibilityEnabled()
        val filesOk    = isFilesGranted()
        val batteryOk  = isBatteryExempt()

        setChip(chipOverlay, overlayOk)
        setChip(chipAccessibility, accessOk)
        setChip(chipFiles, filesOk)
        setChip(chipBattery, batteryOk)

        cardOverlay.isClickable       = !overlayOk
        cardAccessibility.isClickable = !accessOk
        cardFiles.isClickable         = !filesOk
        cardBattery.isClickable       = !batteryOk

        btnContinue.isEnabled = overlayOk && accessOk && filesOk && batteryOk
    }

    private fun setChip(chip: Chip, granted: Boolean) {
        if (granted) {
            chip.text = getString(R.string.granted)
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                getColor(R.color.chip_granted))
            chip.setTextColor(getColor(android.R.color.white))
        } else {
            chip.text = getString(R.string.tap_to_grant)
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                getColor(R.color.chip_pending))
            chip.setTextColor(getColor(android.R.color.black))
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = "${packageName}/${DuressAccessibilityService::class.java.canonicalName}"
        return try {
            val enabled = Settings.Secure.getString(
                contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            enabled.contains(service)
        } catch (_: Exception) { false }
    }

    private fun isFilesGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isBatteryExempt(): Boolean {
        val pm = getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestOverlay() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")))
    }

    private fun requestAccessibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isRestrictedSettingsAllowed()) {
            showRestrictedSettingsDialog()
            return
        }
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun isRestrictedSettingsAllowed(): Boolean {
        return try {
            val ops = getSystemService(android.app.AppOpsManager::class.java)
            val mode = ops.checkOpNoThrow("android:access_restricted_settings",
                android.os.Process.myUid(), packageName)
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { true }
    }

    private fun showRestrictedSettingsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.restricted_settings_title)
            .setMessage(R.string.restricted_settings_message)
            .setPositiveButton(R.string.open_app_info) { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun requestFileAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")))
        } else {
            requestPermissions(arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
        }
    }

    private fun requestBatteryExemption() {
        startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName")))
    }

    private fun proceed() {
        VaultPrefs.markSetupComplete(this)
        WatchdogWorker.schedule(this)
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
