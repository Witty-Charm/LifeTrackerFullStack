package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.domain.auth.AuthRepository
import com.lifetracker.mobile.domain.auth.AuthSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isSigningOut: Boolean = false,
    val accountEmail: String? = null,
    val accountDisplayName: String? = null,
)

class SettingsViewModel(
    private val auth: AuthRepository,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            SettingsUiState(
                accountEmail = (auth.authStateFlow.value as? AuthSessionState.SignedIn)?.email,
                accountDisplayName = (auth.authStateFlow.value as? AuthSessionState.SignedIn)?.displayName,
            ),
        )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun signOut() {
        if (_uiState.value.isSigningOut) return
        _uiState.value = _uiState.value.copy(isSigningOut = true)
        viewModelScope.launch {
            auth.signOut()
        }
    }
}
