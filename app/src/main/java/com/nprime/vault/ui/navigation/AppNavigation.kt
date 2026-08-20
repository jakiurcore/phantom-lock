package com.nprime.vault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.service.LockOverlayService
import com.nprime.vault.ui.home.HomeScreen
import com.nprime.vault.ui.pinsetup.PasswordSetupScreen
import com.nprime.vault.ui.setup.SetupScreen
import com.nprime.vault.ui.targets.TargetsScreen

private const val ROUTE_SETUP      = "setup"
private const val ROUTE_PW_REAL    = "password/real"
private const val ROUTE_PW_DURESS  = "password/duress"
private const val ROUTE_HOME       = "home"
private const val ROUTE_TARGETS    = "targets"
private const val ROUTE_CHANGE_PW  = "change/{mode}"

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val start = remember {
        if (VaultPrefs.isSetupComplete(context)) ROUTE_HOME else ROUTE_SETUP
    }
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = start) {

        composable(ROUTE_SETUP) {
            SetupScreen(
                onSetPasswords = { nav.navigate(ROUTE_PW_REAL) },
                onDone = {
                    nav.navigate(ROUTE_HOME) {
                        popUpTo(ROUTE_SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_PW_REAL) {
            PasswordSetupScreen(
                mode = "real",
                onDone = { nav.navigate(ROUTE_PW_DURESS) }
            )
        }

        composable(ROUTE_PW_DURESS) {
            PasswordSetupScreen(
                mode = "duress",
                onDone = {
                    VaultPrefs.markSetupComplete(context)
                    // Auto-start the lock — no manual enable needed
                    LockOverlayService.start(context)
                    nav.popBackStack(ROUTE_SETUP, inclusive = false)
                }
            )
        }

        composable(ROUTE_HOME) {
            HomeScreen(
                onNavigateTargets = { nav.navigate(ROUTE_TARGETS) },
                onChangePassword = { mode -> nav.navigate("change/$mode") },
                onNavigateSetup = { nav.navigate(ROUTE_SETUP) }
            )
        }

        composable(ROUTE_TARGETS) {
            TargetsScreen()
        }

        composable(ROUTE_CHANGE_PW) { entry ->
            val mode = entry.arguments?.getString("mode") ?: "real"
            PasswordSetupScreen(
                mode = mode,
                onDone = { nav.popBackStack() }
            )
        }
    }
}
