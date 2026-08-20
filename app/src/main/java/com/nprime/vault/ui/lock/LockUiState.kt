package com.nprime.vault.ui.lock

data class LockUiState(
    val isWiping: Boolean = false,        // loading screen visible (app uninstall phase)
    val wipeProgress: Float = 0f,         // 0f..1f for progress bar
    val isBlockingInput: Boolean = false, // invisible overlay (file deletion phase)
    val isError: Boolean = false,
    val lockoutUntil: Long = 0L,
)
