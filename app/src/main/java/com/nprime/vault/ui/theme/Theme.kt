package com.nprime.vault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LockScreenColors = darkColorScheme(
    primary          = White,
    onPrimary        = Black,
    background       = Black,
    onBackground     = White,
    surface          = Gray800,
    onSurface        = White,
    surfaceVariant   = Gray700,
    onSurfaceVariant = Gray400,
    error            = ErrorRed,
    onError          = White
)

@Composable
fun VaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LockScreenColors,
        typography  = VaultTypography,
        content     = content
    )
}
