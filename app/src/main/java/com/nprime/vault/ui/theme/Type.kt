package com.nprime.vault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val VaultTypography = Typography(
    // Lock screen clock
    displayLarge = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize   = 64.sp,
        letterSpacing = (-1).sp
    ),
    // Screen titles  (e.g. "System Settings")
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    // Card titles / sub-section headings
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 20.sp,
        letterSpacing = (-0.2).sp
    ),
    // Lock screen date line
    titleMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        letterSpacing = 0.sp
    ),
    // Row primary text
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 17.sp,
        letterSpacing = (-0.2).sp
    ),
    // Row secondary text / descriptions
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        letterSpacing = 0.sp
    ),
    // Section headers – uppercase, tracked
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        letterSpacing = 1.5.sp
    ),
    // Input field text
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
        letterSpacing = 0.sp
    )
)
