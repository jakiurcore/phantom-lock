package com.nprime.vault.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nprime.vault.R
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.overlay.LockOverlayService
import com.nprime.vault.ui.permissions.PermissionSetupActivity
import com.nprime.vault.ui.pinsetup.PinSetupActivity
import com.nprime.vault.ui.targets.TargetsActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var switchLock: SwitchMaterial
    private lateinit var btnTargets: MaterialButton
    private lateinit var btnPinSetup: MaterialButton
    private lateinit var btnPermissions: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        switchLock     = findViewById(R.id.switch_lock_enabled)
        btnTargets     = findViewById(R.id.btn_select_targets)
        btnPinSetup    = findViewById(R.id.btn_set_codes)
        btnPermissions = findViewById(R.id.btn_permissions)

        btnTargets.setOnClickListener {
            startActivity(Intent(this, TargetsActivity::class.java))
        }
        btnPinSetup.setOnClickListener {
            startActivity(Intent(this, PinSetupActivity::class.java))
        }
        btnPermissions.setOnClickListener {
            startActivity(Intent(this, PermissionSetupActivity::class.java))
        }

        switchLock.setOnCheckedChangeListener { _, checked ->
            if (checked && !VaultPrefs.hasPinsConfigured(this)) {
                setSwitchSilently(false)
                Toast.makeText(this, R.string.set_pins_first, Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            VaultPrefs.setLockEnabled(this, checked)
            // Only stop the service on disable; the receiver handles starting it on unlock.
            if (!checked) LockOverlayService.stop(this)
        }
    }

    override fun onResume() {
        super.onResume()
        // Set the switch without triggering the listener so we don't start the service.
        setSwitchSilently(VaultPrefs.isLockEnabled(this))
    }

    private fun setSwitchSilently(checked: Boolean) {
        switchLock.setOnCheckedChangeListener(null)
        switchLock.isChecked = checked
        switchLock.setOnCheckedChangeListener { _, c ->
            if (c && !VaultPrefs.hasPinsConfigured(this)) {
                setSwitchSilently(false)
                Toast.makeText(this, R.string.set_pins_first, Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            VaultPrefs.setLockEnabled(this, c)
            if (c) LockOverlayService.start(this) else LockOverlayService.stop(this)
        }
    }
}
