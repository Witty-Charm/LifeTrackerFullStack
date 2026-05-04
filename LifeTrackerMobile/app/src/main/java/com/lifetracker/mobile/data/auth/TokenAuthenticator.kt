package com.lifetracker.mobile.data.auth

import com.lifetracker.mobile.data.remote.AuthApi
import com.lifetracker.mobile.data.remote.dto.RefreshRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

/**
 * Reacts to 401 responses on authenticated requests by attempting one refresh,
 * then retrying the original request once with the new access token. If the
 * refresh fails the local session is cleared so the navigator can route the
 * user back to the sign-in screen.
 */
class TokenAuthenticator(
    private val tokenStore: AuthTokenStore,
    private val authApiProvider: () -> AuthApi,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.startsWith("/api/auth/")) {
            return null
        }
        if (responseCount(response) >= 2) {
            return null
        }
        val staleAccessToken = bearerOf(response.request)
        val currentAccessToken = tokenStore.getAccessToken()
        if (currentAccessToken != null && currentAccessToken != staleAccessToken) {
            return retryWith(response.request, currentAccessToken)
        }
        val refreshToken = tokenStore.getRefreshToken() ?: run {
            tokenStore.clear()
            return null
        }
        val refreshed = runCatching {
            runBlocking { authApiProvider().refresh(RefreshRequestDto(refreshToken)) }
        }.getOrElse { error ->
            Timber.w(error, "Refresh request crashed; signing out.")
            tokenStore.clear()
            return null
        }
        if (!refreshed.isSuccessful) {
            Timber.w("Refresh failed with HTTP %d; signing out.", refreshed.code())
            tokenStore.clear()
            return null
        }
        val body = refreshed.body() ?: run {
            tokenStore.clear()
            return null
        }
        tokenStore.save(AuthTokens(body.accessToken, body.refreshToken))
        return retryWith(response.request, body.accessToken)
    }

    private fun retryWith(original: Request, accessToken: String): Request =
        original.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

    private fun bearerOf(request: Request): String? =
        request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.takeIf { it.isNotBlank() }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
