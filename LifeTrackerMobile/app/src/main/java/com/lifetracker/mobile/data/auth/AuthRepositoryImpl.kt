package com.lifetracker.mobile.data.auth

import androidx.work.WorkManager
import com.lifetracker.mobile.core.sync.SyncScheduler
import com.lifetracker.mobile.data.local.dao.HeroDao
import com.lifetracker.mobile.data.local.dao.TaskDao
import com.lifetracker.mobile.data.remote.AuthApi
import com.lifetracker.mobile.data.remote.dto.AuthResponseDto
import com.lifetracker.mobile.data.remote.dto.GoogleSignInRequestDto
import com.lifetracker.mobile.data.remote.dto.LogoutRequestDto
import com.lifetracker.mobile.domain.auth.AuthRepository
import com.lifetracker.mobile.domain.auth.AuthSessionState
import com.lifetracker.mobile.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val tokenStore: AuthTokenStore,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
    private val heroDao: HeroDao,
    private val taskDao: TaskDao,
    private val workManager: WorkManager,
) : AuthRepository {
    private val sessionState: MutableStateFlow<AuthSessionState> =
        MutableStateFlow(initialStateFromTokens())

    init {
        scope.launch {
            tokenStore.authStateFlow.collect { tokenState ->
                if (tokenState is AuthState.SignedOut) {
                    sessionState.value = AuthSessionState.SignedOut
                }
            }
        }
    }

    override val authStateFlow: StateFlow<AuthSessionState> = sessionState.asStateFlow()

    override suspend fun signInWithGoogle(idToken: String): Result<AuthSessionState.SignedIn> =
        withContext(Dispatchers.IO) {
            runCatching {
                val claimDeviceId = settings.getOrCreateDeviceIdBlocking()
                val response =
                    authApi.signInWithGoogle(
                        GoogleSignInRequestDto(idToken = idToken, claimDeviceId = claimDeviceId),
                    )
                val body = unwrap(response)
                tokenStore.save(AuthTokens(body.accessToken, body.refreshToken))
                val signedIn = AuthSessionState.SignedIn(
                    userId = body.user.id,
                    email = body.user.email,
                    displayName = body.user.displayName,
                )
                sessionState.value = signedIn
                signedIn
            }.onFailure { Timber.w(it, "Google sign-in failed.") }
        }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            val refreshToken = tokenStore.getRefreshToken()
            if (!refreshToken.isNullOrBlank()) {
                runCatching {
                    authApi.logout(LogoutRequestDto(refreshToken))
                }.onFailure { Timber.w(it, "Logout request failed; clearing tokens locally.") }
            }
            tokenStore.clear()
            runCatching { workManager.cancelUniqueWork(SyncScheduler.WORK_NAME) }
                .onFailure { Timber.w(it, "Failed to cancel sync work on sign-out.") }
            runCatching {
                taskDao.deleteAll()
                heroDao.deleteAll()
            }.onFailure { Timber.w(it, "Failed to clear local data on sign-out.") }
            sessionState.value = AuthSessionState.SignedOut
        }
    }

    private fun unwrap(response: Response<AuthResponseDto>): AuthResponseDto {
        if (!response.isSuccessful) {
            error("Auth request failed with HTTP ${response.code()}")
        }
        return response.body() ?: error("Auth response had no body")
    }

    private fun initialStateFromTokens(): AuthSessionState =
        if (tokenStore.getAccessToken().isNullOrBlank()) {
            AuthSessionState.SignedOut
        } else {
            AuthSessionState.Unknown
        }
}
