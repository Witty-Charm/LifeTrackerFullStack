package com.lifetracker.mobile.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.lifetracker.mobile.data.auth.AuthInterceptor
import com.lifetracker.mobile.data.auth.AuthTokenStore
import com.lifetracker.mobile.data.auth.TokenAuthenticator
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {
    fun provideAuthOkHttpClient(
        tokenStore: AuthTokenStore,
        deviceIdProvider: () -> String,
        isDebug: Boolean = false,
    ): OkHttpClient =
        baseClientBuilder(isDebug)
            .addInterceptor(AuthInterceptor(tokenStore, deviceIdProvider))
            .build()

    fun provideOkHttpClient(
        tokenStore: AuthTokenStore,
        deviceIdProvider: () -> String,
        authApiProvider: () -> AuthApi,
        isDebug: Boolean = false,
    ): OkHttpClient =
        baseClientBuilder(isDebug)
            .addInterceptor(AuthInterceptor(tokenStore, deviceIdProvider))
            .authenticator(TokenAuthenticator(tokenStore, authApiProvider))
            .build()

    fun provideRetrofit(
        baseUrl: String,
        client: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    fun provideApi(retrofit: Retrofit): LifeTrackerApi = retrofit.create(LifeTrackerApi::class.java)

    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    private fun baseClientBuilder(isDebug: Boolean): OkHttpClient.Builder {
        val builder =
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
        if (isDebug) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
        }
        return builder
    }
}
