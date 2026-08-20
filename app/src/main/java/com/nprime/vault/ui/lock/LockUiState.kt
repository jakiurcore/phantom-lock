package com.nprime.vault.ui.lock

data class LockUiState(
    val isWiping: Boolean = false,
    val isError: Boolean = false,
    val lockoutUntil: Long = 0L,
    val wipeMessage: String = "Decrypting device…"
)
