package com.nprime.vault.ui.pinsetup

import android.content.Context
import androidx.lifecycle.ViewModel
import com.nprime.vault.data.VaultPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PasswordSetupUiState(
    val step: Int = 0,            // 0 = enter, 1 = confirm
    val isError: Boolean = false,
    val errorMessage: String = "",
    val isDone: Boolean = false
)

class PasswordSetupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordSetupUiState())
    val uiState = _uiState.asStateFlow()

    private var firstEntry = ""

    fun onSubmit(value: String, context: Context, mode: String) {
        when (_uiState.value.step) {
            0 -> {
                if (value.length < 6) {
                    _uiState.update { it.copy(isError = true, errorMessage = "Minimum 6 characters") }
                    return
                }
                firstEntry = value
                _uiState.update { it.copy(step = 1, isError = false, errorMessage = "") }
            }
            1 -> {
                if (value != firstEntry) {
                    firstEntry = ""
                    _uiState.update { it.copy(step = 0, isError = true, errorMessage = "Passwords don't match") }
                    return
                }
                if (mode == "real") VaultPrefs.saveRealPin(context, value)
                else VaultPrefs.saveDuressPin(context, value)
                _uiState.update { it.copy(isDone = true) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(isError = false) }
    }
}
