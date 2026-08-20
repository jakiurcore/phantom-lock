package com.nprime.vault.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nprime.vault.ui.theme.ErrorRed
import com.nprime.vault.ui.theme.SuccessGreen

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
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("System Settings", style = MaterialTheme.typography.headlineSmall, color = Color.White)

        // Status card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), MaterialTheme.shapes.medium)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusRow("Device Owner", state.isDeviceOwner)
            StatusRow("Lock Active", state.isLockEnabled)
        }

        // Lock toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), MaterialTheme.shapes.medium)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Lock", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = state.isLockEnabled,
                onCheckedChange = { vm.setLockEnabled(context, it) },
                enabled = state.isDeviceOwner,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color.White
                )
            )
        }

        // ADB block toggle — warning: disables ADB on device
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), MaterialTheme.shapes.medium)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Block ADB / Debug", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Warning: disables adb on this device",
                    color = Color(0xFF888888),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = state.isAdbBlocked,
                onCheckedChange = { vm.setAdbBlocked(context, it) },
                enabled = state.isDeviceOwner,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = ErrorRed
                )
            )
        }

        // Targets card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), MaterialTheme.shapes.medium)
                .clickable { onNavigateTargets() }
                .padding(16.dp)
        ) {
            Text("Duress Targets", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${state.selectedApps} apps · ${state.selectedFiles} file paths",
                color = Color(0xFF888888),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Password buttons
        Button(
            onClick = { onChangePassword("real") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
        ) { Text("Change Password", color = Color.White) }

        Button(
            onClick = { onChangePassword("duress") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
        ) { Text("Change Duress Password", color = Color.White) }

        Spacer(modifier = Modifier.weight(1f))

        // Deactivate
        Button(
            onClick = { vm.showDeactivateDialog(true) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C0000))
        ) { Text("Deactivate Vault", color = ErrorRed) }
    }

    if (state.showDeactivateDialog) {
        AlertDialog(
            onDismissRequest = { vm.showDeactivateDialog(false) },
            title = { Text("Deactivate Vault?", color = Color.White) },
            text = {
                Text(
                    "This will remove Device Owner status and allow the app to be uninstalled. The lock screen will stop working.",
                    color = Color(0xFF888888)
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.deactivate(context) }) {
                    Text("Deactivate", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.showDeactivateDialog(false) }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}

@Composable
private fun StatusRow(label: String, active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF888888), style = MaterialTheme.typography.bodyMedium)
        Text(
            if (active) "ACTIVE" else "INACTIVE",
            color = if (active) SuccessGreen else ErrorRed,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
