package com.nprime.vault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nprime.vault.service.LockOverlayService
import com.nprime.vault.ui.navigation.AppNavigation
import com.nprime.vault.ui.theme.VaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VaultTheme {
                AppNavigation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        from(this)
    }

    companion object {
        fun from(activity: ComponentActivity) {
            val ctx = activity.applicationContext
            val prefs = com.nprime.vault.data.VaultPrefs
            if (prefs.isSetupComplete(ctx)) {
                LockOverlayService.start(ctx)
            }
        }
    }
}
