package com.nprime.vault.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nprime.vault.ui.components.ChevronTrailing
import com.nprime.vault.ui.components.SectionHeader
import com.nprime.vault.ui.components.SettingsRow
import com.nprime.vault.ui.components.ToggleRow
import com.nprime.vault.ui.theme.Danger
import com.nprime.vault.ui.theme.Divider
import com.nprime.vault.ui.theme.Success
import com.nprime.vault.ui.theme.Surface
import com.nprime.vault.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onNavigateTargets: () -> Unit,
    onChangePassword: (String) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    LaunchedEffect(Unit) { vm.refresh(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(top = 56.dp, bottom = 40.dp)
    ) {
        // Title
        Text(
            text = "System Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(28.dp))

        // ── SECURITY ─────────────────────────────────────────────────────────
        SectionHeader("Security")
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
        ) {
            SettingsRow(
                title = "Device Owner",
                trailing = {
                    Text(
                        if (state.isDeviceOwner) "Active" else "Inactive",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isDeviceOwner) Success else Danger
                    )
                },
                showDivider = true
            )
            SettingsRow(
                title = "Lock Screen",
                trailing = {
                    Text(
                        if (state.isLockEnabled) "On" else "Off",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isLockEnabled) Success else TextSecondary
                    )
                },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── LOCK ─────────────────────────────────────────────────────────────
        SectionHeader("Lock")
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
        ) {
            ToggleRow(
                title = "Enable Lock Screen",
                checked = state.isLockEnabled,
                enabled = state.isDeviceOwner,
                showDivider = false,
                onCheckedChange = { vm.setLockEnabled(context, it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── PROTECTION ───────────────────────────────────────────────────────
        SectionHeader("Protection")
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
        ) {
            ToggleRow(
                title = "Block ADB & Debugging",
                subtitle = "Warning: disables adb on this device",
                checked = state.isAdbBlocked,
                enabled = state.isDeviceOwner,
                danger = true,
                showDivider = false,
                onCheckedChange = { vm.setAdbBlocked(context, it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── DURESS TARGETS ───────────────────────────────────────────────────
        SectionHeader("Duress Targets")
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
        ) {
            SettingsRow(
                title = "Apps",
                trailing = {
                    Text(
                        "${state.selectedApps} selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    ChevronTrailing()
                },
                onClick = onNavigateTargets,
                showDivider = true
            )
            SettingsRow(
                title = "Files & Folders",
                trailing = {
                    Text(
                        "${state.selectedFiles} paths",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    ChevronTrailing()
                },
                onClick = onNavigateTargets,
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── PASSWORDS ────────────────────────────────────────────────────────
        SectionHeader("Passwords")
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
        ) {
            SettingsRow(
                title = "Change Password",
                trailing = { ChevronTrailing() },
                onClick = { onChangePassword("real") },
                showDivider = true
            )
            SettingsRow(
                title = "Change Duress Password",
                trailing = { ChevronTrailing() },
                onClick = { onChangePassword("duress") },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── DANGER ZONE ──────────────────────────────────────────────────────
        SectionHeader("Danger Zone")
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A0000))
        ) {
            SettingsRow(
                title = "Deactivate Vault",
                subtitle = "Removes Device Owner and stops all protection",
                titleColor = Danger,
                trailing = { ChevronTrailing() },
                onClick = { vm.showDeactivateDialog(true) },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Deactivating allows the app to be uninstalled.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }

    if (state.showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { vm.showDeactivateDialog(false) },
            title = { Text("Deactivate Vault?", color = Color.White) },
            text = {
                Text(
                    "This removes Device Owner status and stops lock screen protection. The app can then be uninstalled.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.deactivate(context) }) {
                    Text("Deactivate", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.showDeactivateDialog(false) }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Surface
        )
    }
}
