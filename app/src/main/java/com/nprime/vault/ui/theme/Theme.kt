package com.nprime.vault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VaultColors = darkColorScheme(
    primary            = Accent,
    onPrimary          = White,
    background         = Black,
    onBackground       = White,
    surface            = Surface,
    onSurface          = White,
    surfaceVariant     = SurfaceHigh,
    onSurfaceVariant   = TextSecondary,
    error              = Danger,
    onError            = White,
    outline            = Divider,
    outlineVariant     = Gray700
)

@Composable
fun VaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VaultColors,
        typography  = VaultTypography,
        content     = content
    )
}
