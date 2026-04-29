package com.lifetracker.mobile.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {
    private val deviceId = "11111111-1111-1111-1111-111111111111"

    @Test
    fun adds_bearer_and_device_id_when_token_available() {
        val interceptor = AuthInterceptor(FakeTokenStore("at"), { deviceId })
        val captured = capture(interceptor, "/api/heroes/current")

        assertEquals("Bearer at", captured.header("Authorization"))
        assertEquals(deviceId, captured.header("X-Device-Id"))
    }

    @Test
    fun omits_bearer_when_token_blank_but_keeps_device_id() {
        val interceptor = AuthInterceptor(FakeTokenStore(null), { deviceId })
        val captured = capture(interceptor, "/api/heroes/current")

        assertNull(captured.header("Authorization"))
        assertEquals(deviceId, captured.header("X-Device-Id"))
    }

    @Test
    fun does_not_attach_authorization_to_auth_endpoints() {
        val interceptor = AuthInterceptor(FakeTokenStore("at"), { deviceId })
        val captured = capture(interceptor, "/api/auth/google")

        assertNull(captured.header("Authorization"))
        assertEquals(deviceId, captured.header("X-Device-Id"))
    }

    private fun capture(interceptor: AuthInterceptor, path: String): Request {
        val request = Request.Builder().url("http://localhost$path").build()
        var captured: Request = request
        val chain =
            object : Interceptor.Chain {
                override fun call() = throw UnsupportedOperationException()

                override fun connectTimeoutMillis() = 0

                override fun readTimeoutMillis() = 0

                override fun writeTimeoutMillis() = 0

                override fun connection() = null

                override fun proceed(req: Request): Response {
                    captured = req
                    return Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(204)
                        .message("OK")
                        .body("".toResponseBody(null))
                        .build()
                }

                override fun request() = request

                override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this

                override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this

                override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            }
        interceptor.intercept(chain)
        return captured
    }

    private class FakeTokenStore(private val accessToken: String?) : AuthTokenStore {
        override val authStateFlow: StateFlow<AuthState> = MutableStateFlow(AuthState.SignedOut)

        override fun getAccessToken(): String? = accessToken

        override fun getRefreshToken(): String? = null

        override fun save(tokens: AuthTokens) = Unit

        override fun clear() = Unit
    }
}
