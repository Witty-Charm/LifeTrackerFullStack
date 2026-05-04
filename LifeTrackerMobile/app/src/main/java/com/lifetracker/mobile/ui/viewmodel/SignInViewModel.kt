package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.data.auth.GoogleSignInClient
import com.lifetracker.mobile.domain.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignInUiState(
    val isSigningIn: Boolean = false,
    val error: String? = null,
)

class SignInViewModel(
    private val auth: AuthRepository,
    private val googleSignInClient: GoogleSignInClient,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun startSignIn() {
        if (_uiState.value.isSigningIn) return
        _uiState.value = SignInUiState(isSigningIn = true)
        viewModelScope.launch {
            val tokenResult = googleSignInClient.fetchIdToken()
            val idToken =
                tokenResult.getOrElse {
                    _uiState.value = SignInUiState(isSigningIn = false, error = it.message ?: "Sign-in cancelled")
                    return@launch
                }
            val signInResult = auth.signInWithGoogle(idToken)
            signInResult.fold(
                onSuccess = {
                    _uiState.value = SignInUiState(isSigningIn = false)
                },
                onFailure = {
                    _uiState.value = SignInUiState(isSigningIn = false, error = it.message ?: "Sign-in failed")
                },
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
