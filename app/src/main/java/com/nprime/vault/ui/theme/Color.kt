package com.nprime.vault.ui.theme

import androidx.compose.ui.graphics.Color

// ── Surfaces ──────────────────────────────────────────────────────────────────
val Black         = Color(0xFF000000)   // true OLED black – background
val Surface       = Color(0xFF111111)   // card / list group background
val SurfaceHigh   = Color(0xFF1C1C1C)   // elevated rows, code blocks
val Divider       = Color(0xFF282828)   // subtle row separators

// ── Text ──────────────────────────────────────────────────────────────────────
val White         = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8E8E93)   // secondary labels (iOS system gray)
val TextTertiary  = Color(0xFF48484A)   // disabled / placeholder

// ── Semantic ──────────────────────────────────────────────────────────────────
val Accent        = Color(0xFF0A7AFF)   // system blue  – active toggles, links
val Danger        = Color(0xFFFF3B30)   // system red   – danger actions
val Success       = Color(0xFF30D158)   // system green – active/confirmed states
val Warning       = Color(0xFFFF9F0A)   // system orange

// ── Lock-screen only ──────────────────────────────────────────────────────────
val ErrorRed      = Color(0xFFCF6679)   // shake indicator on wrong password
val Gray400       = Color(0xFFB3B3B3)   // clock date line
val Gray700       = Color(0xFF2C2C2C)   // unfocused input border
val Gray800       = Color(0xFF1A1A1A)   // input surface (kept for PasswordField)
