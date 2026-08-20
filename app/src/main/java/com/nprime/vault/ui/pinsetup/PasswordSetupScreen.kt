package com.nprime.vault.ui.pinsetup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nprime.vault.ui.components.PasswordField
import com.nprime.vault.ui.theme.ErrorRed

@Composable
fun PasswordSetupScreen(
    mode: String,             // "real" or "duress"
    onDone: () -> Unit,
    vm: PasswordSetupViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    var password by remember(state.step) { mutableStateOf("") }

    LaunchedEffect(state.isDone) {
        if (state.isDone) onDone()
    }

    val title = if (mode == "real") "Set your password" else "Set duress password"
    val subtitle = when {
        mode == "duress" && state.step == 0 -> "Entering this unlocks the phone and silently wipes selected data"
        state.step == 0 -> "Choose a strong password"
        else -> "Enter the same password again to confirm"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .imePadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF888888))

        Spacer(modifier = Modifier.height(40.dp))

        if (state.isError && state.errorMessage.isNotEmpty()) {
            Text(
                state.errorMessage,
                color = ErrorRed,
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
