package com.nprime.vault.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nprime.vault.admin.DeviceOwnerManager
import com.nprime.vault.ui.theme.Accent
import com.nprime.vault.ui.theme.Danger
import com.nprime.vault.ui.theme.Divider
import com.nprime.vault.ui.theme.Success
import com.nprime.vault.ui.theme.Surface
import com.nprime.vault.ui.theme.SurfaceHigh
import com.nprime.vault.ui.theme.TextSecondary

@Composable
fun SetupScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var isOwner by remember { mutableStateOf(DeviceOwnerManager.isDeviceOwner(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (isOwner) Success.copy(alpha = 0.15f) else Danger.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isOwner) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isOwner) Success else Danger,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isOwner) "Device Owner Active" else "Device Owner Required",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isOwner)
                "Ready to configure your lock screen."
            else
                "Vault requires Device Owner to replace the system keyguard.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        if (!isOwner) {
            Spacer(modifier = Modifier.height(32.dp))

            // ADB command block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Accent)
                    )
                    Text("Step 1", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text(
                    "Remove all Google accounts from device Settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                Box(modifier = Modifier.height(0.5.dp).fillMaxWidth().background(Divider))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Accent)
                    )
                    Text("Step 2", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text(
                    "Run via ADB",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceHigh)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "adb shell dpm set-device-owner\n  com.nprime.vault/.admin.DeviceOwnerReceiver",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF98C379)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!isOwner) {
            OutlinedButton(
                onClick = { isOwner = DeviceOwnerManager.isDeviceOwner(context) },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Divider)
            ) {
                Text("Check Status", color = Color.White)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onContinue,
            enabled = isOwner,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color.White,
                disabledContainerColor = Surface,
                disabledContentColor = TextSecondary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
