package com.nprime.vault

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nprime.vault.data.VaultPrefs
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
        if (VaultPrefs.isSetupComplete(this)) {
            LockOverlayService.start(this)
            hideLauncherIcon()
        }
    }

    /** Removes the app icon from the launcher once setup is complete. */
    private fun hideLauncherIcon() {
        val alias = ComponentName(this, "${packageName}.MainActivityAlias")
        if (packageManager.getComponentEnabledSetting(alias) ==
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED) return
        packageManager.setComponentEnabledSetting(
            alias,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
