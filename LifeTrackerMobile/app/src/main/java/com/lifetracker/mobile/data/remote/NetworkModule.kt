package com.lifetracker.mobile.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {
    fun provideOkHttpClient(
        deviceIdProvider: () -> String,
        isDebug: Boolean = false,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(addDeviceIdHeader(chain.request(), deviceIdProvider()))
            }.apply {
                if (isDebug) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        },
                    )
                }
            }.build()

    fun addDeviceIdHeader(
        request: Request,
        deviceId: String,
    ): Request =
        request.newBuilder()
            .header("X-Device-Id", deviceId)
            .build()

    fun provideApi(
        baseUrl: String,
        client: OkHttpClient,
        json: Json,
    ): LifeTrackerApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LifeTrackerApi::class.java)
}
