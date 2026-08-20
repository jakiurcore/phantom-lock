package com.nprime.vault.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nprime.vault.ui.components.PasswordField
import com.nprime.vault.ui.theme.ErrorRed
import com.nprime.vault.ui.theme.Gray400
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun LockScreen(
    state: LockUiState,
    onSubmit: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Tick clock every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = LocalDateTime.now()
            nowMs = System.currentTimeMillis()
        }
    }

    // Clear password after error animation completes
    LaunchedEffect(state.isError) {
        if (state.isError) {
            delay(700)
            password = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.isWiping) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                Text(text = state.wipeMessage, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Clock
                Text(
                    text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                    style = MaterialTheme.typography.titleMedium,
                    color = Gray400
                )

                Spacer(modifier = Modifier.height(56.dp))

                // Lockout countdown
                val isLockedOut = state.lockoutUntil > nowMs
                if (isLockedOut) {
                    val seconds = ((state.lockoutUntil - nowMs) / 1000).coerceAtLeast(1)
                    Text(
                        text = "Too many attempts — try again in ${seconds}s",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Password input
                PasswordField(
                    value = password,
                    onValueChange = { if (!isLockedOut) password = it },
                    onSubmit = { if (!isLockedOut) onSubmit(password) },
                    label = "Password",
                    isError = state.isError,
                    autoFocus = true
                )
            }
        }
    }
}
