package com.nprime.vault.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.nprime.vault.ui.theme.ErrorRed
import com.nprime.vault.ui.theme.Gray400
import com.nprime.vault.ui.theme.Gray700
import com.nprime.vault.ui.theme.White
import kotlinx.coroutines.launch

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    label: String = "Password",
    isError: Boolean = false,
    autoFocus: Boolean = true,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    // Shake animation on error
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(isError) {
        if (isError) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            launch {
                for (i in 0..4) {
                    shakeOffset.animateTo(if (i % 2 == 0) 18f else -18f, tween(50))
                }
                shakeOffset.animateTo(0f, tween(50))
            }
        }
    }

    if (autoFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onValueChange(new)
        },
        label = { Text(label, color = if (isError) ErrorRed else Gray400) },
        singleLine = true,
        isError = isError,
        visualTransformation = if (visible) VisualTransformation.None
                               else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.Visibility
                                  else Icons.Filled.VisibilityOff,
                    contentDescription = null,
                    tint = Gray400
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor      = White,
            unfocusedTextColor    = White,
            focusedBorderColor    = White,
            unfocusedBorderColor  = Gray700,
            errorBorderColor      = ErrorRed,
            cursorColor           = White,
            errorCursorColor      = ErrorRed
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .graphicsLayer { translationX = shakeOffset.value }
    )
}
