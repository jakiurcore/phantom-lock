package com.nprime.vault.ui.pinsetup

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nprime.vault.ui.components.PasswordField
import com.nprime.vault.ui.theme.Accent
import com.nprime.vault.ui.theme.Danger
import com.nprime.vault.ui.theme.TextSecondary

@Composable
fun PasswordSetupScreen(
    mode: String,
    onDone: () -> Unit,
    vm: PasswordSetupViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    var password by remember(state.step) { mutableStateOf("") }

    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }

    val isDuress = mode == "duress"
    val stepCount = 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Step dots
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(stepCount) { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == state.step) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                i < state.step  -> Accent
                                i == state.step -> Color.White
                                else            -> Color(0xFF3A3A3C)
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = when {
                isDuress && state.step == 0 -> "Set Duress Password"
                isDuress && state.step == 1 -> "Confirm Duress Password"
                state.step == 0             -> "Set Your Password"
                else                        -> "Confirm Password"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                isDuress && state.step == 0 ->
                    "Entering this password unlocks the phone and silently wipes your selected data."
                state.step == 1 ->
                    "Enter the same password again to confirm."
                else ->
                    "Choose a strong password — minimum 6 characters."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (state.isError && state.errorMessage.isNotEmpty()) {
            Text(
                state.errorMessage,
                color = Danger,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        PasswordField(
            value = password,
            onValueChange = {
                password = it
                if (state.isError) vm.clearError()
            },
            onSubmit = { vm.onSubmit(password, context, mode) },
            label = if (state.step == 0) "Password" else "Confirm password",
            isError = state.isError,
            autoFocus = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
