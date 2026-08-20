package com.nprime.vault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val VaultTypography = Typography(
    // Clock on lock screen
    displayLarge = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        letterSpacing = (-1).sp,
        color = White
    ),
    // Date line under clock
    titleMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        color = Gray400
    ),
    // Input field text
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        letterSpacing = 0.5.sp,
        color = White
    ),
    // Hint / label text
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        color = Gray400
    )
)
