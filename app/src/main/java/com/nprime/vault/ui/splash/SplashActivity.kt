package com.nprime.vault.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.ui.home.HomeActivity
import com.nprime.vault.ui.onboarding.OnboardingActivity
import com.nprime.vault.ui.permissions.PermissionSetupActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val next = when {
            !VaultPrefs.isOnboardingDone(this) -> OnboardingActivity::class.java
            !VaultPrefs.isSetupComplete(this) -> PermissionSetupActivity::class.java
            else -> HomeActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }
}
