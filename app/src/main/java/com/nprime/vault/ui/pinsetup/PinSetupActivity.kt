package com.nprime.vault.ui.pinsetup

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nprime.vault.R
import com.nprime.vault.data.VaultPrefs

class PinSetupActivity : AppCompatActivity() {

    private lateinit var tilReal: TextInputLayout
    private lateinit var tilRealConfirm: TextInputLayout
    private lateinit var tilDuress: TextInputLayout
    private lateinit var tilDuressConfirm: TextInputLayout
    private lateinit var etReal: TextInputEditText
    private lateinit var etRealConfirm: TextInputEditText
    private lateinit var etDuress: TextInputEditText
    private lateinit var etDuressConfirm: TextInputEditText
    private lateinit var btnSave: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_setup)

        tilReal         = findViewById(R.id.til_real_pin)
        tilRealConfirm  = findViewById(R.id.til_real_pin_confirm)
        tilDuress       = findViewById(R.id.til_duress_pin)
        tilDuressConfirm = findViewById(R.id.til_duress_pin_confirm)
        etReal          = findViewById(R.id.et_real_pin)
        etRealConfirm   = findViewById(R.id.et_real_pin_confirm)
        etDuress        = findViewById(R.id.et_duress_pin)
        etDuressConfirm = findViewById(R.id.et_duress_pin_confirm)
        btnSave         = findViewById(R.id.btn_save_pins)

        btnSave.setOnClickListener { validate() }
    }

    private fun validate() {
        val real        = etReal.text?.toString().orEmpty().trim()
        val realConf    = etRealConfirm.text?.toString().orEmpty().trim()
        val duress      = etDuress.text?.toString().orEmpty().trim()
        val duressConf  = etDuressConfirm.text?.toString().orEmpty().trim()

        var valid = true

        if (real.length < 4) {
            tilReal.error = getString(R.string.pin_min_length); valid = false
        } else { tilReal.error = null }

        if (real != realConf) {
            tilRealConfirm.error = getString(R.string.pins_dont_match); valid = false
        } else { tilRealConfirm.error = null }

        if (duress.length < 4) {
            tilDuress.error = getString(R.string.pin_min_length); valid = false
        } else { tilDuress.error = null }

        if (duress != duressConf) {
            tilDuressConfirm.error = getString(R.string.pins_dont_match); valid = false
        } else { tilDuressConfirm.error = null }

        if (real == duress) {
            tilDuress.error = getString(R.string.pins_must_differ); valid = false
        }

        if (!valid) return

        VaultPrefs.saveRealPin(this, real)
        VaultPrefs.saveDuressPin(this, duress)
        Toast.makeText(this, R.string.pins_saved, Toast.LENGTH_SHORT).show()
        finish()
    }
}
