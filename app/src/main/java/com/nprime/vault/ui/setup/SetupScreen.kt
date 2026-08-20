package com.nprime.vault.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nprime.vault.admin.DeviceOwnerManager
import com.nprime.vault.ui.theme.ErrorRed
import com.nprime.vault.ui.theme.SuccessGreen

@Composable
fun SetupScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var isOwner by remember { mutableStateOf(DeviceOwnerManager.isDeviceOwner(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "System Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Device administrator activation required",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF888888)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Status card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A), shape = MaterialTheme.shapes.medium)
                .padding(20.dp)
        ) {
            Text(
                text = if (isOwner) "Device Owner: ACTIVE" else "Device Owner: INACTIVE",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isOwner) SuccessGreen else ErrorRed
            )

            if (!isOwner) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Run this command via ADB after removing all accounts:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "adb shell dpm set-device-owner com.nprime.vault/.admin.DeviceOwnerReceiver",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCCCCCC),
                    modifier = Modifier
                        .background(Color(0xFF2C2C2C), MaterialTheme.shapes.small)
                        .padding(12.dp)
                        .fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isOwner) {
            OutlinedButton(
                onClick = { isOwner = DeviceOwnerManager.isDeviceOwner(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check Status")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onContinue,
            enabled = isOwner,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF2C2C2C),
                disabledContentColor = Color(0xFF666666)
            )
        ) {
            Text("Continue")
        }
    }
}
