package com.lifetracker.mobile.data.auth

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Adds the Authorization: Bearer header (when a token is available) and the
 * legacy X-Device-Id header (always, so the backend can claim orphan heroes
 * on first sign-in and keep per-device analytics).
 */
class AuthInterceptor(
    private val tokenStore: AuthTokenStore,
    private val deviceIdProvider: () -> String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (isAuthEndpoint(request)) {
            return chain.proceed(decorate(request, accessToken = null))
        }
        val token = tokenStore.getAccessToken()
        return chain.proceed(decorate(request, accessToken = token))
    }

    private fun decorate(request: Request, accessToken: String?): Request {
        val builder = request.newBuilder().header("X-Device-Id", deviceIdProvider())
        if (!accessToken.isNullOrBlank() && request.header("Authorization") == null) {
            builder.header("Authorization", "Bearer $accessToken")
        }
        return builder.build()
    }

    private fun isAuthEndpoint(request: Request): Boolean {
        val path = request.url.encodedPath
        return path.startsWith("/api/auth/")
    }
}
