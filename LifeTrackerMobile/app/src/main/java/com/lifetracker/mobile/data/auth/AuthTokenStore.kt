package com.lifetracker.mobile.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the access + refresh tokens that back the authenticated API session.
 * Stored via EncryptedSharedPreferences (AES-256 GCM, master key in AndroidKeyStore).
 */
interface AuthTokenStore {
    val authStateFlow: StateFlow<AuthState>

    fun getAccessToken(): String?

    fun getRefreshToken(): String?

    fun save(tokens: AuthTokens)

    fun clear()
}

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

sealed interface AuthState {
    data object Unknown : AuthState

    data object SignedOut : AuthState

    data class SignedIn(val accessToken: String) : AuthState
}

class EncryptedAuthTokenStore(context: Context) : AuthTokenStore {
    private val prefs: SharedPreferences = run {
        val masterKey =
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _authStateFlow: MutableStateFlow<AuthState> =
        MutableStateFlow(currentAuthState())

    override val authStateFlow: StateFlow<AuthState> = _authStateFlow.asStateFlow()

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)

    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    override fun save(tokens: AuthTokens) {
        prefs.edit()
            .putString(KEY_ACCESS, tokens.accessToken)
            .putString(KEY_REFRESH, tokens.refreshToken)
            .apply()
        _authStateFlow.value = AuthState.SignedIn(tokens.accessToken)
    }

    override fun clear() {
        prefs.edit().clear().apply()
        _authStateFlow.value = AuthState.SignedOut
    }

    private fun currentAuthState(): AuthState {
        val access = prefs.getString(KEY_ACCESS, null)
        return if (access.isNullOrBlank()) AuthState.SignedOut else AuthState.SignedIn(access)
    }

    private companion object {
        const val PREFS_FILE = "lifetracker_auth"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
