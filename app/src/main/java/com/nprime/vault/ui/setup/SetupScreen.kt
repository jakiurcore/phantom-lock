package com.nprime.vault.ui.setup

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.nprime.vault.data.VaultPrefs
import com.nprime.vault.ui.theme.Accent
import com.nprime.vault.ui.theme.Danger
import com.nprime.vault.ui.theme.Divider
import com.nprime.vault.ui.theme.Success
import com.nprime.vault.ui.theme.Surface
import com.nprime.vault.ui.theme.SurfaceHigh
import com.nprime.vault.ui.theme.TextSecondary
import com.nprime.vault.ui.theme.Warning

private enum class StepStatus { DONE, ACTIVE, LOCKED }

@Composable
fun SetupScreen(
    onSetPasswords: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current

    var isOwner     by remember { mutableStateOf(false) }
    var lockRemoved by remember { mutableStateOf(false) }
    var hasOverlay  by remember { mutableStateOf(false) }
    var hasPins     by remember { mutableStateOf(false) }

    fun refresh() {
        isOwner     = DeviceOwnerManager.isDeviceOwner(context)
        lockRemoved = !DeviceOwnerManager.isSystemLockScreenSecure(context)
        hasOverlay  = Settings.canDrawOverlays(context)
        hasPins     = VaultPrefs.hasPinsSet(context)
    }

    LaunchedEffect(Unit) { refresh() }

    val allDone = isOwner && lockRemoved && hasOverlay && hasPins

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 40.dp)
    ) {
        Text(
            text = "Setup",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Text(
            text = "Complete each step to activate Vault's lock screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Step 1: Device Owner ─────────────────────────────────────────────
        val s1 = if (isOwner) StepStatus.DONE else StepStatus.ACTIVE
        StepCard(
            number = 1,
            title = "Provision Device Owner",
            status = s1
        ) {
            Text(
                "Device Owner (DO) grants Vault the power to disable the system keyguard " +
                "and silently uninstall apps on duress.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "1. Remove all Google accounts from Settings → Accounts",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("2. Connect USB and run:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            CodeBlock("adb shell dpm set-device-owner\n  com.nprime.vault/.admin.DeviceOwnerReceiver")
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { refresh() },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Divider)
            ) { Text("Check Status", color = Color.White) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Step 2: Remove system lock ───────────────────────────────────────
        val s2 = when {
            lockRemoved -> StepStatus.DONE
            isOwner     -> StepStatus.ACTIVE
            else        -> StepStatus.LOCKED
        }
        StepCard(
            number = 2,
            title = "Remove System Lock Screen",
            status = s2
        ) {
            Text(
                "Android cannot disable the system keyguard while a PIN, pattern, password, " +
                "or fingerprint is configured. Vault becomes the only lock screen once the " +
                "system one is removed.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Settings → Security & Privacy → Screen Lock → None",
                style = MaterialTheme.typography.bodySmall,
                color = Warning
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_SECURITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceHigh),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Open Security Settings", color = Color.White) }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { refresh() },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Divider)
            ) { Text("Check Status", color = Color.White) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Step 3: Overlay permission ───────────────────────────────────────
        val s3 = when {
            hasOverlay             -> StepStatus.DONE
            isOwner && lockRemoved -> StepStatus.ACTIVE
            else                   -> StepStatus.LOCKED
        }
        StepCard(
            number = 3,
            title = "Allow Display Over Other Apps",
            status = s3
        ) {
            Text(
                "Vault draws the lock screen using a system overlay window. " +
                "This permission lets it appear above everything, including the launcher.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceHigh),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Open Permission Settings", color = Color.White) }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { refresh() },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Divider)
            ) { Text("Check Status", color = Color.White) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Step 4: Set passwords ────────────────────────────────────────────
        val s4 = when {
            hasPins                             -> StepStatus.DONE
            isOwner && lockRemoved && hasOverlay -> StepStatus.ACTIVE
            else                                 -> StepStatus.LOCKED
        }
        StepCard(
            number = 4,
            title = "Set Unlock Passwords",
            status = s4
        ) {
            Text(
                "Set your real unlock password and a duress password. " +
                "Entering the duress password silently wipes selected apps and files.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onSetPasswords() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Set Passwords") }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (allDone) {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Success),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp))
                Text("Activate Vault", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    status: StepStatus,
    content: @Composable () -> Unit
) {
    val isActive = status == StepStatus.ACTIVE
    val isDone   = status == StepStatus.DONE
    val isLocked = status == StepStatus.LOCKED

    val borderColor = when (status) {
        StepStatus.DONE   -> Success.copy(alpha = 0.4f)
        StepStatus.ACTIVE -> Accent.copy(alpha = 0.6f)
        StepStatus.LOCKED -> Color.Transparent
    }
    val badgeBg = when (status) {
        StepStatus.DONE   -> Success
        StepStatus.ACTIVE -> Accent
        StepStatus.LOCKED -> Color(0xFF2C2C2E)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isLocked) 0.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .background(if (isLocked) Color(0xFF0A0A0A) else Surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isLocked) TextSecondary else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isLocked) TextSecondary else Color.White
            )

            if (isDone) {
                Spacer(modifier = Modifier.weight(1f))
                Text("Done", style = MaterialTheme.typography.labelSmall, color = Success)
            }
        }

        AnimatedVisibility(
            visible = !isDone,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceHigh)
            .padding(12.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF98C379)
        )
    }
}

