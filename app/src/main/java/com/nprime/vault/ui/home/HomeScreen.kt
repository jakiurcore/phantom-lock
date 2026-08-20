package com.nprime.vault.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.ui.components.ChevronTrailing
import com.nprime.vault.ui.components.SectionHeader
import com.nprime.vault.ui.components.SettingsRow
import com.nprime.vault.ui.components.ToggleRow
import com.nprime.vault.ui.theme.Accent
import com.nprime.vault.ui.theme.Danger
import com.nprime.vault.ui.theme.Success
import com.nprime.vault.ui.theme.Surface
import com.nprime.vault.ui.theme.TextSecondary
import com.nprime.vault.ui.theme.Warning

@Composable
fun HomeScreen(
    onNavigateTargets: () -> Unit,
    onChangePassword: (String) -> Unit,
    onNavigateSetup: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    LaunchedEffect(Unit) { vm.refresh(context) }

    val setupOk = state.isDeviceOwner && !state.systemLockSecure && state.hasOverlayPermission

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(top = 56.dp, bottom = 40.dp)
    ) {
        Text(
            "System Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(28.dp))

        // ── Setup incomplete banner ───────────────────────────────────────────
        if (!setupOk) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C1A00))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning, null,
                    tint = Warning, modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Setup incomplete", style = MaterialTheme.typography.bodyLarge, color = Warning)
                    Text(
                        buildSetupStatus(state),
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                }
                ChevronTrailing()
            }
            TextButton(
                onClick = onNavigateSetup,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) { Text("View Setup Steps", color = Accent) }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Security ─────────────────────────────────────────────────────────
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
                title = "System Keyguard",
                trailing = {
                    Text(
                        if (!state.systemLockSecure) "Disabled" else "Active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!state.systemLockSecure) Success else Danger
                    )
                },
                showDivider = true
            )
            SettingsRow(
                title = "Lock Screen",
                trailing = {
                    Text(
                        if (state.isLockRunning) "Running" else "Stopped",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isLockRunning) Success else Danger
                    )
                },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Protection ───────────────────────────────────────────────────────
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

        // ── Security Policy ───────────────────────────────────────────────────
        SectionHeader("Security Policy")
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
        ) {
            SettingsRow(
                title = "Wipe after failed attempts",
                subtitle = "Factory reset if this many wrong passwords entered",
                showDivider = false,
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Minus button
                        IconButton(
                            onClick = { vm.setMaxAttempts(context, state.maxAttempts - 1) },
                            enabled = state.maxAttempts > VaultPrefs.MIN_ATTEMPTS,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (state.maxAttempts > VaultPrefs.MIN_ATTEMPTS) Surface else Color(0xFF111111))
                        ) {
                            Text(
                                "−",
                                color = if (state.maxAttempts > VaultPrefs.MIN_ATTEMPTS) Color.White else TextSecondary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "${state.maxAttempts}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Danger,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        // Plus button
                        IconButton(
                            onClick = { vm.setMaxAttempts(context, state.maxAttempts + 1) },
                            enabled = state.maxAttempts < VaultPrefs.MAX_ATTEMPTS_LIMIT,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (state.maxAttempts < VaultPrefs.MAX_ATTEMPTS_LIMIT) Surface else Color(0xFF111111))
                        ) {
                            Text(
                                "+",
                                color = if (state.maxAttempts < VaultPrefs.MAX_ATTEMPTS_LIMIT) Color.White else TextSecondary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            )
        }
        Text(
            "Range: ${VaultPrefs.MIN_ATTEMPTS}–${VaultPrefs.MAX_ATTEMPTS_LIMIT}. " +
            "On the ${state.maxAttempts}th wrong attempt the device factory resets.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // ── Duress Targets ───────────────────────────────────────────────────
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

        // ── Passwords ────────────────────────────────────────────────────────
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
    }
}

private fun buildSetupStatus(state: HomeUiState): String {
    val issues = mutableListOf<String>()
    if (!state.isDeviceOwner)        issues.add("Device Owner not provisioned")
    if (state.systemLockSecure)      issues.add("System lock screen still active")
    if (!state.hasOverlayPermission) issues.add("Overlay permission missing")
    return issues.joinToString(" · ")
}
