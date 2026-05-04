package com.lifetracker.mobile.domain.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authStateFlow: StateFlow<AuthSessionState>

    suspend fun signInWithGoogle(idToken: String): Result<AuthSessionState.SignedIn>

    suspend fun signOut()
}

sealed interface AuthSessionState {
    data object Unknown : AuthSessionState

    data object SignedOut : AuthSessionState

    data class SignedIn(val userId: Int, val email: String, val displayName: String?) : AuthSessionState
}
